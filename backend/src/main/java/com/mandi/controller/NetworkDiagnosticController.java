package com.mandi.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NetworkDiagnosticController {

    @Value("${mandi.api.key:}")
    private String apiKey;

    @GetMapping("/api/diagnostic/data-gov")
    public Map<String, Object> testDataGovConfiguration() {
        return Map.of(
                "keyConfigured", apiKey != null && !apiKey.isBlank(),
                "keyLength", apiKey == null ? 0 : apiKey.length()
        );
    }
}
