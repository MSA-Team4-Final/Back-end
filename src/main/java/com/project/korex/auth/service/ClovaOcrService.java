package com.project.korex.auth.service;

import com.project.korex.auth.dto.request.ImageParsingRequest;
import com.project.korex.auth.dto.response.OcrData;
import org.apache.tomcat.util.json.JSONParser;
import org.apache.tomcat.util.json.ParseException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ClovaOcrService {

    public static String SECRET = "ZUJhcHZaZUJJamZkVERRSHFQTXJPSWFUdkRoWW9Ub0k=";
    public static String API_URL = "https://9h1f5kkm72.apigw.ntruss.com/custom/v1/45783/2b897cc81562886e21c58f70bc184737357d21200f34bf6ed4ecc22bb78a4931/general";

    public static String execute(ImageParsingRequest request) {
        try {
            URL url = new URL(API_URL);
            HttpURLConnection connection = createRequestHeader(url);
            createRequestBody(connection, request);

            StringBuilder response = getResponseData(connection);
            return parseResponseData(response).toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public OcrData parseOcrResult(String ocrResult) {
        OcrData data = new OcrData();
        if (ocrResult == null) return data;

        // 이름
        Pattern namePattern = Pattern.compile("주민등록[증중]\\s*([가-힣]{2,4})");
        Matcher nameMatcher = namePattern.matcher(ocrResult);
        if(nameMatcher.find()) data.setName(nameMatcher.group(1));

        // RRN / 생년월일
        Pattern rrnPattern = Pattern.compile("(\\d{6}-\\d{7})");
        Matcher rrnMatcher = rrnPattern.matcher(ocrResult);
        if (rrnMatcher.find()) {
            data.setRrn(rrnMatcher.group(1));
            data.setBirth(rrnMatcher.group(1).substring(0,6));
        }

        return data;
    }

    private static HttpURLConnection createRequestHeader(URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setUseCaches(false);
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setReadTimeout(5000);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json;");
        connection.setRequestProperty("X-OCR-SECRET", SECRET);
        return connection;
    }

    private static void createRequestBody(HttpURLConnection connection, ImageParsingRequest request) throws IOException {
        JSONObject image = new JSONObject();
        image.put("format", "PNG");
        image.put("name", "requestImage");

        // URL 있으면 URL, 없으면 Base64
        if (request.url() != null) image.put("url", request.url());
        else image.put("data", request.base64Data());

        JSONArray images = new JSONArray();
        images.put(image);

        JSONObject requestObject = new JSONObject();
        requestObject.put("version", "V2");
        requestObject.put("requestId", UUID.randomUUID().toString());
        requestObject.put("timestamp", System.currentTimeMillis());
        requestObject.put("lang", "ko");
        requestObject.put("resultType", "string");
        requestObject.put("images", images);

        connection.connect();
        try (DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream())) {
            outputStream.write(requestObject.toString().getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
        }
    }

    private static BufferedReader checkResponse(HttpURLConnection connection) throws IOException {
        int responseCode = connection.getResponseCode();
        if (responseCode == 200)
            return new BufferedReader(new InputStreamReader(connection.getInputStream()));
        else
            return new BufferedReader(new InputStreamReader(connection.getErrorStream()));
    }

    private static StringBuilder getResponseData(HttpURLConnection connection) throws IOException {
        BufferedReader reader = checkResponse(connection);
        String line;
        StringBuilder response = new StringBuilder();
        while ((line = reader.readLine()) != null) response.append(line);
        reader.close();
        return response;
    }

    private static StringBuilder parseResponseData(StringBuilder response) throws ParseException {
        JSONParser parser = new JSONParser(response.toString());
        LinkedHashMap<String, Object> map = (LinkedHashMap<String, Object>) parser.parse();
        JSONObject json = new JSONObject(map);

        JSONArray parsedImages = (JSONArray) json.opt("images");
        StringBuilder result = new StringBuilder();
        if (parsedImages != null && !parsedImages.isEmpty()) {
            JSONObject parsedImage = (JSONObject) parsedImages.get(0);
            JSONArray fields = (JSONArray) parsedImage.opt("fields");
            if (fields != null) {
                for (int i = 0; i < fields.length(); i++) {
                    JSONObject field = (JSONObject) fields.get(i);
                    result.append(field.optString("inferText", "")).append(" ");
                }
            }
        }
        return result;
    }

}