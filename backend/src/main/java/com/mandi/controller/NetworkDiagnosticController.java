package com.mandi.controller;

import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NetworkDiagnosticController {

    @GetMapping("/api/diagnostic/data-gov")
    public Map<String, Object> testDataGovConnectivity() {
        try {
            URI uri = URI.create("https://api.data.gov.in");
            HttpURLConnection connection =
                    (HttpURLConnection) uri.toURL().openConnection();

            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            int status = connection.getResponseCode();

            return Map.of(
                    "reachable", true,
                    "status", status
            );

        } catch (Exception e) {
            return Map.of(
                    "reachable", false,
                    "error", e.getClass().getSimpleName(),
                    "message", e.getMessage() == null ? "" : e.getMessage()
            );
        }
    }
}
