package com.mandi.controller;

import com.mandi.config.MandiProperties;
import com.mandi.dto.HistoricalPriceResponse;
import com.mandi.dto.PriceType;
import com.mandi.service.FilterValidator;
import com.mandi.service.HistoricalPriceService;
import com.mandi.service.HistorySyncService;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/mandi")
public class HistoricalPriceController {
    private final HistoricalPriceService history;
    private final HistorySyncService sync;
    private final FilterValidator v;
    private final MandiProperties props;

    public HistoricalPriceController(HistoricalPriceService history, HistorySyncService sync,
                                     FilterValidator v, MandiProperties props) {
        this.history = history; this.sync = sync; this.v = v; this.props = props;
    }

    @GetMapping("/history")
    public HistoricalPriceResponse history(
            @RequestParam String state, @RequestParam String district, @RequestParam String commodity,
            @RequestParam(required = false) String market,
            @RequestParam(required = false) String variety, @RequestParam(required = false) String grade,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String priceType,
            HttpServletRequest request) {
        // read repeated ?markets=A&markets=B raw so market names containing commas stay intact
        String[] raw = request.getParameterValues("markets");
        List<String> markets = raw == null ? List.of()
                : Arrays.stream(raw).map(String::trim).filter(s -> !s.isEmpty()).toList();
        return history.history(new HistoricalPriceService.Request(
                v.requireState(state), v.require(district, "district"), v.require(commodity, "commodity"),
                v.optional(market), markets, v.variantPart(variety), v.variantPart(grade),
                from, to, PriceType.parse(priceType)));
    }

    /** Manual sync trigger. Disabled unless MANDI_ADMIN_TOKEN is set; requires header X-Admin-Token. */
    @PostMapping("/sync")
    public ResponseEntity<Map<String, String>> sync(
            @RequestHeader(value = "X-Admin-Token", required = false) String token,
            @RequestParam(required = false) List<String> states) {
        String expected = props.sync().adminToken();
        if (expected == null || expected.isBlank()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Not available.");
        if (token == null || !MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8), expected.getBytes(StandardCharsets.UTF_8)))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden.");
        List<String> canonical = states == null ? null : states.stream().map(v::requireState).toList();
        boolean started = sync.startAsync(canonical);
        return started ? ResponseEntity.accepted().body(Map.of("status", "started"))
                       : ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("status", "already running"));
    }
}
