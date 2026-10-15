package com.mandi.service;

import com.mandi.config.MandiProperties;
import com.mandi.dto.PriceQuery;
import com.mandi.exception.BadRequestException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class FilterValidator {
    public static final int DEFAULT_LIMIT = 20, MAX_LIMIT = 100;

    private final List<String> states;
    private final Map<String, String> canonical = new HashMap<>();

    public FilterValidator(MandiProperties props) {
        this.states = props.states();
        states.forEach(s -> canonical.put(s.toLowerCase(Locale.ROOT), s));
    }

    public List<String> states() { return states; }

    public String requireState(String s) {
        if (s == null || s.isBlank()) throw new BadRequestException("state is required.");
        String c = canonical.get(s.trim().toLowerCase(Locale.ROOT));
        if (c == null) throw new BadRequestException("Unknown state: " + s.trim());
        return c;
    }

    public String require(String s, String name) {
        if (s == null || s.isBlank()) throw new BadRequestException(name + " is required.");
        return s.trim();
    }

    /** blank / "All" => null (no filter). */
    public String optional(String s) {
        if (s == null) return null;
        String t = s.trim();
        return (t.isEmpty() || t.equalsIgnoreCase("all")) ? null : t;
    }

    /** For variety/grade in history & compare: blank => not chosen, "-" => explicitly empty value. */
    public String variantPart(String s) {
        if (s == null || s.isBlank()) return null;
        return s.trim().equals("-") ? "" : s.trim();
    }

    public int limit(Integer l) {
        if (l == null) return DEFAULT_LIMIT;
        if (l < 1 || l > MAX_LIMIT) throw new BadRequestException("limit must be between 1 and " + MAX_LIMIT + ".");
        return l;
    }

    public int offset(Integer o) {
        if (o == null) return 0;
        if (o < 0) throw new BadRequestException("offset must not be negative.");
        return o;
    }

    public PriceQuery priceQuery(String state, String district, String market, String commodity,
                                 String variety, String grade, Integer limit, Integer offset) {
        return new PriceQuery(requireState(state), optional(district), optional(market), optional(commodity),
                optional(variety), optional(grade), limit(limit), offset(offset));
    }
}
