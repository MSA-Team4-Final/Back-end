package com.project.korex.exchangeRate.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class NaverNewsApiService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${naver.client.id}")
    private String clientId;

    @Value("${naver.client.secret}")
    private String clientSecret;

    private static final String NAVER_NEWS_API_URL = "https://openapi.naver.com/v1/search/news.json";

    /**
     * 네이버 뉴스 API로 환율 관련 최신 뉴스 가져오기
     */
    public List<Map<String, String>> getLatestExchangeNewsFromApi() {
        return getLatestExchangeNewsFromApi("달러 환율, 환율 상승, 환율 하락", 50);
    }

    /**
     * 네이버 뉴스 API로 특정 키워드의 최신 뉴스 가져오기
     */
    public List<Map<String, String>> getLatestExchangeNewsFromApi(String keyword, int displayCount) {
        List<Map<String, String>> newsList = new ArrayList<>();

        try {
            String query = URLEncoder.encode(keyword, "UTF-8");
            String apiURL = NAVER_NEWS_API_URL +
                    "?query=" + query +
                    "&display=" + displayCount +  // 검색결과 개수 (최대 100)
                    "&start=1" +                   // 검색 시작 위치
                    "&sort=date";                  // 날짜순 정렬 (최신순)

            URL url = new URL(apiURL);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("X-Naver-Client-Id", clientId);
            con.setRequestProperty("X-Naver-Client-Secret", clientSecret);
            con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

            int responseCode = con.getResponseCode();
            BufferedReader br;

            if (responseCode == 200) {
                br = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));
            } else {
                br = new BufferedReader(new InputStreamReader(con.getErrorStream(), "UTF-8"));
                System.err.println("네이버 뉴스 API 호출 실패. 응답 코드: " + responseCode);
                return newsList;
            }

            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = br.readLine()) != null) {
                response.append(inputLine);
            }
            br.close();
            con.disconnect();

            // JSON 파싱
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(response.toString());
            JsonNode itemsNode = rootNode.get("items");

            if (itemsNode != null && itemsNode.isArray()) {
                for (JsonNode item : itemsNode) {
                    Map<String, String> newsData = new LinkedHashMap<>();

                    // 데이터 추출 및 정리
                    String title = cleanHtmlTags(item.get("title").asText());
                    String description = cleanHtmlTags(item.get("description").asText());
                    String link = item.get("link").asText();
                    String originalLink = item.get("originallink").asText();
                    String pubDate = formatDate(item.get("pubDate").asText());

                    // 빈 데이터 체크
                    if (title.isEmpty() || link.isEmpty()) {
                        continue;
                    }

                    newsData.put("title", title);
                    newsData.put("summary", description);
                    newsData.put("link", link);
                    newsData.put("original_link", originalLink);
                    newsData.put("date", pubDate);
                    newsData.put("press", extractPressName(originalLink));

                    newsList.add(newsData);
                }
            }

        } catch (Exception e) {
            System.err.println("네이버 뉴스 API 호출 실패: " + e.getMessage());
            e.printStackTrace();
        }

        return newsList;
    }

    /**
     * HTML 태그 및 특수문자 제거
     */
    private String cleanHtmlTags(String text) {
        if (text == null) return "";

        return text.replaceAll("<[^>]+>", "")           // HTML 태그 제거
                .replaceAll("&quot;", "\"")           // HTML 엔티티 변환
                .replaceAll("&amp;", "&")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&nbsp;", " ")
                .replaceAll("\\s+", " ")              // 연속 공백 제거
                .trim();
    }

    /**
     * 날짜 포맷 변경 (RFC 2822 → 사용자 친화적 형태)
     */
    private String formatDate(String pubDate) {
        try {
            // RFC 2822 형식: "Thu, 21 Aug 2025 14:30:00 +0900"
            SimpleDateFormat inputFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
            SimpleDateFormat outputFormat = new SimpleDateFormat("MM-dd HH:mm");
            Date date = inputFormat.parse(pubDate);
            return outputFormat.format(date);
        } catch (Exception e) {
            System.err.println("날짜 파싱 실패: " + pubDate);
            return pubDate; // 파싱 실패시 원본 반환
        }
    }

    /**
     * 원본 링크에서 언론사명 추출
     */
    private String extractPressName(String originalLink) {
        if (originalLink == null || originalLink.isEmpty()) {
            return "네이버뉴스";
        }

        try {
            URL url = new URL(originalLink);
            String domain = url.getHost();

            // 주요 언론사 도메인 매핑
            Map<String, String> pressMapping = new HashMap<>();
            pressMapping.put("yna.co.kr", "연합뉴스");
            pressMapping.put("chosun.com", "조선일보");
            pressMapping.put("donga.com", "동아일보");
            pressMapping.put("joongang.co.kr", "중앙일보");
            pressMapping.put("hankyung.com", "한국경제");
            pressMapping.put("mk.co.kr", "매일경제");
            pressMapping.put("ebn.co.kr", "EBN");
            pressMapping.put("ziksir.com", "직썰");
            pressMapping.put("news2day.co.kr", "뉴스투데이");

            for (Map.Entry<String, String> entry : pressMapping.entrySet()) {
                if (domain.contains(entry.getKey())) {
                    return entry.getValue();
                }
            }

            // 매핑되지 않은 경우 도메인명 반환
            return domain.replace("www.", "");

        } catch (Exception e) {
            return "네이버뉴스";
        }
    }
}
