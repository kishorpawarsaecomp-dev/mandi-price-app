package com.mandi.service;

import com.mandi.client.DataGovMandiClient;
import com.mandi.client.DataGovPage;
import com.mandi.dto.ComparisonResponse;
import com.mandi.dto.ComparisonResponse.Row;
import com.mandi.dto.MandiPriceRecord;
import com.mandi.dto.VariantOption;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/** Latest reported price per market (live API, cached). Same variety+grade only; never mixes them. */
@Service
public class MarketComparisonService {
    private static final int MAX_ROWS = 500;
    private final DataGovMandiClient client;

    public MarketComparisonService(DataGovMandiClient client) { this.client = client; }

    @Cacheable(cacheNames = "compare", key = "#state + '|' + #district + '|' + #commodity + '|' + #variety + '|' + #grade")
    public ComparisonResponse compare(String state, String district, String commodity, String variety, String grade) {
        Map<String, String> f = new LinkedHashMap<>();
        f.put("state", state); f.put("district", district); f.put("commodity", commodity);
        DataGovPage page = client.fetch(f, MAX_ROWS, 0);
        List<MandiPriceRecord> all = page.records();

        // Variants ordered by frequency; choose the requested one, else the most common.
        Map<VariantOption, Long> counts = all.stream().collect(
                Collectors.groupingBy(r -> new VariantOption(r.variety(), r.grade()), LinkedHashMap::new, Collectors.counting()));
        List<VariantOption> variants = counts.entrySet().stream()
                .sorted(Map.Entry.<VariantOption, Long>comparingByValue().reversed()).map(Map.Entry::getKey).toList();
        VariantOption chosen = variants.stream()
                .filter(v -> variety == null || v.variety().equalsIgnoreCase(variety))
                .filter(v -> grade == null || v.grade().equalsIgnoreCase(grade))
                .findFirst().orElse(null);
        if (chosen == null) {
            return new ComparisonResponse(state, district, commodity, variety, grade, null, List.of(), variants, MandiPriceService.SOURCE);
        }

        Map<String, MandiPriceRecord> latestPerMarket = new LinkedHashMap<>();
        for (MandiPriceRecord r : all) {
            if (!r.variety().equals(chosen.variety()) || !r.grade().equals(chosen.grade())) continue;
            MandiPriceRecord cur = latestPerMarket.get(r.market());
            if (cur == null || r.arrivalDate().isAfter(cur.arrivalDate())) latestPerMarket.put(r.market(), r);
        }
        List<Row> rows = latestPerMarket.values().stream()
                .map(r -> new Row(r.market(), r.modalPrice(), r.minPrice(), r.maxPrice(), r.arrivalDate()))
                .sorted(Comparator.comparing(Row::modalPrice).reversed()).toList();
        var latest = rows.stream().map(Row::arrivalDate).max(Comparator.naturalOrder()).orElse(null);
        return new ComparisonResponse(state, district, commodity, chosen.variety(), chosen.grade(), latest, rows,
                variants, MandiPriceService.SOURCE);
    }
}
