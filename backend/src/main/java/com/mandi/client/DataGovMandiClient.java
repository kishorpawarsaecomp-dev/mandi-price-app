package com.mandi.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.mandi.config.MandiProperties;
import com.mandi.dto.MandiPriceRecord;
import com.mandi.exception.ExternalApiException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * The ONLY class that knows data.gov.in's wire format. If the upstream renames/adds fields,
 * fix it here (field lookup is case-insensitive and tolerant of unknown fields).
 */
@Component
public class DataGovMandiClient {
    private static final Logger log = LoggerFactory.getLogger(DataGovMandiClient.class);
    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final RestClient rest;
    private final MandiProperties props;

    public DataGovMandiClient(RestClient dataGovRestClient, MandiProperties props) {
        this.rest = dataGovRestClient;
        this.props = props;
    }

    /** filters keys: state, district, market, commodity, variety, grade. */
    public DataGovPage fetch(Map<String, String> filters, int limit, int offset) {
        String key = props.api().key();
        if (key == null || key.isBlank()) {
            log.error("DATA_GOV_API_KEY is not configured");
            throw new ExternalApiException("API key not configured");
        }
        URI uri = buildUri(filters, limit, offset, key);
        log.info("Mandi API request started filters={} limit={} offset={}", filters, limit, offset);
        JsonNode root = execute(uri, key);
        DataGovPage page = parse(root);
        log.info("Mandi API request successful total={} received={} invalid={}",
                page.total(), page.rawCount(), page.invalidCount());
        return page;
    }

    private URI buildUri(Map<String, String> filters, int limit, int offset, String key) {
        StringBuilder sb = new StringBuilder(props.api().baseUrl()).append('/').append(props.api().resourceId())
                .append("?api-key=").append(enc(key)).append("&format=json")
                .append("&limit=").append(limit).append("&offset=").append(offset);
        filters.forEach((field, value) -> {
            if (value != null && !value.isBlank()) {
                sb.append('&').append(enc("filters[" + field + ".keyword]")).append('=').append(enc(value));
            }
        });
        return URI.create(sb.toString());
    }

    private JsonNode execute(URI uri, String key) {
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                return rest.get().uri(uri).retrieve().body(JsonNode.class);
            } catch (HttpClientErrorException e) {          // 4xx: retrying will not help
                log.warn("Mandi API rejected request: HTTP {}", e.getStatusCode().value());
                throw new ExternalApiException("Upstream rejected request: HTTP " + e.getStatusCode().value());
            } catch (RestClientException e) {                // 5xx, timeouts, bad body: retry once
                log.warn("Mandi API attempt {} failed: {}", attempt, sanitize(e.getMessage(), key));
            }
        }
        throw new ExternalApiException("Mandi API unavailable after retries");
    }

    DataGovPage parse(JsonNode root) {
        JsonNode records = root == null ? null : root.get("records");
        if (records == null || !records.isArray()) {
            String status = root == null ? "" : root.path("status").asText("");
            throw new ExternalApiException("Malformed response from mandi API" + (status.isEmpty() ? "" : " (status=" + status + ")"));
        }
        List<MandiPriceRecord> out = new ArrayList<>();
        int invalid = 0;
        for (JsonNode n : records) {
            MandiPriceRecord r = map(n);
            if (r == null) invalid++; else out.add(r);
        }
        long total = root.path("total").asLong(records.size());
        return new DataGovPage(total, out, records.size(), invalid);
    }

    /** Returns null when the row is unusable (never "fixes" data by inventing values). */
    private MandiPriceRecord map(JsonNode n) {
        Map<String, String> f = new HashMap<>();
        n.fields().forEachRemaining(e -> f.put(e.getKey().toLowerCase(Locale.ROOT),
                e.getValue().isNull() ? null : e.getValue().asText().trim()));
        String state = f.get("state"), district = f.get("district"), market = f.get("market"),
                commodity = f.get("commodity");
        if (blank(state) || blank(district) || blank(market) || blank(commodity)) return null;
        LocalDate date = parseDate(f.get("arrival_date"));
        BigDecimal modal = price(f.get("modal_price"));
        if (date == null || modal == null || date.isAfter(LocalDate.now().plusDays(1))) return null;
        BigDecimal min = price(f.get("min_price")), max = price(f.get("max_price"));
        if (min != null && max != null && min.compareTo(max) > 0) { min = null; max = null; } // contradictory
        return new MandiPriceRecord(state, district, market, commodity,
                nz(f.get("variety")), nz(f.get("grade")), date, min, max, modal);
    }

    static BigDecimal price(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            BigDecimal v = new BigDecimal(s.trim());
            return v.signum() > 0 ? v : null;               // null, 0, negatives, "NR"/text => not a price
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static LocalDate parseDate(String s) {
        if (s == null) return null;
        try { return LocalDate.parse(s.trim(), DMY); } catch (DateTimeParseException ignored) { }
        try { return LocalDate.parse(s.trim()); } catch (DateTimeParseException ignored) { return null; }
    }

    private static boolean blank(String s) { return s == null || s.isBlank(); }
    private static String nz(String s) { return s == null ? "" : s; }
    private static String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }

    private static String sanitize(String msg, String key) {
        if (msg == null) return "";
        return msg.replace(key, "***").replace(enc(key), "***");
    }
}
