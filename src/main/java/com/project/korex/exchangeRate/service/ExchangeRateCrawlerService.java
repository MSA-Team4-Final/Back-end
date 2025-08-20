package com.project.korex.exchangeRate.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ExchangeRateCrawlerService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    private static final String REDIS_KEY = "exchange:realtime";
    private static final String REDIS_KEY_PREFIX = "exchange:realtime:";

    private static final String URL_REALTIME = "https://finance.naver.com/marketindex/exchangeList.naver";
    private static final String URL_DAILY = "https://finance.naver.com/marketindex/exchangeDailyQuote.naver?marketindexCd=FX_%sKRW&page=%d";

    // 30초마다 크롤링 후 각 통화별 Redis 리스트에 저장
    @Scheduled(fixedRate = 30000)
    public void scheduledCrawlAndCache() {
        try {
            List<Map<String, String>> exchangeList = crawlRealtimeRate();
            // 현재 시간 추가
            String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

            for (Map<String, String> rateData : exchangeList) {
                String currencyCode = rateData.get("currency_code");
                if (currencyCode != null && !currencyCode.isEmpty()) {
                    // 시간 정보 추가
                    rateData.put("crawl_time", currentTime);

                    // 해당 key(통화 코드)에 Redis 저장
                    saveRealtimeData(currencyCode, rateData);
                }
            }
            System.out.println("crawling completed: " + currentTime + ", num of currency: " + exchangeList.size());

        } catch (IOException e) {
            System.err.println("환율 크롤링 실패: " + e.getMessage());
        }
    }

    // 통화코드별 Redis 키 생성 및 데이터 저장 (최신 10개 유지)
    public void saveRealtimeData(String currencyCode, Map<String, String> newData) {
        String redisKey = REDIS_KEY_PREFIX + currencyCode;
        redisTemplate.opsForList().leftPush(redisKey, newData);
        redisTemplate.opsForList().trim(redisKey, 0, 50);  // 최신 50개만 유지
    }

    /**
     * 실시간 환율 데이터 크롤링
     * 크롤링 후 redis 캐싱
     */
    public List<Map<String, String>> crawlRealtimeRate() throws IOException {

        Document doc = Jsoup.connect(URL_REALTIME).userAgent("Mozilla/5.0").get();

        Elements rows = doc.select("table.tbl_exchange > tbody > tr");

        // JSON 배열로 만들기 위해 List<Map<String, String>>
        List<Map<String, String>> exchangeList = new ArrayList<>();

        // 컬럼명 정의
        String[] fieldNames = {
                "base_rate",     // 기준 환율
                "buy_cash_rate",      // 현찰 살 때
                "sell_cash_rate",     // 현찰 팔 때
                "send_rate",         // 송금 보낼 때
                "receive_rate",      // 송금 받을 때
                "usd_conversion_rate"  // 미화환산율
        };

        for (Element row : rows) {
            // 통화코드 추출
            String fullText = row.select("td.tit").text().trim();
            String currencyCode = fullText.replaceAll("[^A-Za-z]", "");

            Elements tds = row.select("td"); // 모든 td 요소

            Map<String, String> rateData = new LinkedHashMap<>();
            rateData.put("currency_code", currencyCode);

            for (int i = 1; i <= 6; i++) {
                String key = fieldNames[i - 1];
                String value = tds.get(i).text().trim();
                rateData.put(key, value);
            }

            exchangeList.add(rateData);

        } // end for

        // 크롤링 끝난 후 캐시에 저장 (ex: TTL 3분)
        redisTemplate.opsForValue().set(REDIS_KEY, exchangeList, Duration.ofMinutes(3));
        return exchangeList;
    }

    /**
     * Redis에서 환율정보 직접 조회
     */

    // 전체 통화 코드 실시간 조회
    public List<Map<String, String>> getRealtimeRateFromCache() {
        return (List<Map<String, String>>) redisTemplate.opsForValue().get(REDIS_KEY);
    }

    // 특정 통화코드별로 최신 10개 환율 데이터 조회
    @SuppressWarnings("unchecked")
    public List<Map<String, String>> getRealtimeCurrencyRateFromCache(String currencyCode) {
        String redisKey = REDIS_KEY_PREFIX + currencyCode;
        List<Object> cachedList = redisTemplate.opsForList().range(redisKey, 0, 50);

        if (cachedList == null) return Collections.emptyList();

        List<Map<String, String>> result = new ArrayList<>();
        for (Object obj : cachedList) {
            if (obj instanceof Map) {
                result.add((Map<String, String>) obj);
            }
        }
        return result;
    }


    /**
     * 과거 데이터 크롤링
     * page기반으로 가져옴(10개씩)
     */
    public List<Map<String, String>> crawlDailyRate(String currencyCode, int page) throws IOException {

        String url = String.format(URL_DAILY, currencyCode, page);
        Document doc = Jsoup.connect(url).userAgent("Mozilla/5.0").get();

        Elements rows = doc.select("table.tbl_exchange > tbody > tr");
        System.out.println("jjh" + rows);

        // JSON 배열로 만들기 위해 List<Map<String, String>>
        List<Map<String, String>> exchangeList = new ArrayList<>();

        // 컬럼명 정의
        String[] fieldNames = {
                "base_date", // 날짜
                "base_rate",     // 매매 기준율
                "change_direction", // 전일대비 방향
                "change_amount",    // 전일대비 변화량
                "buy_cash_rate",      // 현찰 살 때
                "sell_cash_rate",     // 현찰 팔 때
                "send_rate",         // 송금 보낼 때
                "receive_rate",      // 송금 받을 때
        };

        for (Element row : rows) {
            Elements tds = row.select("td");
            // td 개수 체크 등 방어코드는 생략(필요가 있으면 추가)
            if (tds.size() < 7) continue; // 최소 td 개수 확인

            Map<String, String> rateData = new LinkedHashMap<>();

            // 0: base_date
            rateData.put(fieldNames[0], tds.get(0).text().trim());

            // 1: base_rate
            rateData.put(fieldNames[1], tds.get(1).text().trim());

            // 2: changeDirection / changeAmount
            Element diffTd = tds.get(2);
            String changeDirection = "";
            Element img = diffTd.selectFirst("img");
            if (img != null) {
                String alt = img.attr("alt"); // "상승" or "하락" 등
                changeDirection = alt;
            }
            rateData.put(fieldNames[2], changeDirection);

            // 전일대비 변화량은 숫자 부분의 텍스트만 추출
            String changeAmount = diffTd.ownText().trim();
            rateData.put(fieldNames[3], changeAmount);

            // 3~6: buy/sell/send/receive
            for (int i = 3; i <= 6; i++) {
                rateData.put(fieldNames[i + 1], tds.get(i).text().trim()); // i+1 : fieldNames 순서 맞추기
            }

            exchangeList.add(rateData);
        }

        System.out.println(exchangeList);

        return exchangeList;
    }

}

