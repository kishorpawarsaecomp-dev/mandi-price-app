package com.mandi.controller;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NetworkDiagnosticController {

    @Value("${mandi.api.key:}")
    private String apiKey;

    @GetMapping("/api/diagnostic/data-gov")
    public Map<String, Object> testDataGovRequest() {
        try {
            String encodedKey = URLEncoder.encode(apiKey, StandardCharsets.UTF_8);

            String url =
                "https://api.data.gov.in/resource/9ef84268-d588-465a-a308-a864a43d0070"
                + "?api-key=" + encodedKey
                + "&format=json"
                + "&limit=1";

            HttpURLConnection connection =
                    (HttpURLConnection) URI.create(url).toURL().openConnection();

            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(15000);

            int status = connection.getResponseCode();

            return Map.of(
                    "reachable", true,
                    "httpStatus", status,
                    "keyConfigured", apiKey != null && !apiKey.isBlank(),
                    "keyLength", apiKey == null ? 0 : apiKey.length()
            );

        } catch (Exception e) {
            return Map.of(
                    "reachable", false,
                    "keyConfigured", apiKey != null && !apiKey.isBlank(),
                    "keyLength", apiKey == null ? 0 : apiKey.length(),
                    "error", e.getClass().getSimpleName(),
                    "message", e.getMessage() == null ? "" : e.getMessage()
            );
        }
    }
}
