package com.mandi.service;

import com.mandi.client.DataGovMandiClient;
import com.mandi.client.DataGovPage;
import com.mandi.config.MandiProperties;
import com.mandi.dto.MandiPriceRecord;
import com.mandi.dto.OptionsResponse;
import com.mandi.dto.VariantOption;
import java.util.*;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Dropdown values are discovered from what the API actually returns for the chosen filters
 * (nothing is hard-coded). The scan is capped (mandi.discovery.max-records) and cached for hours.
 */
@Service
public class OptionsService {
    private final DataGovMandiClient client;
    private final MandiProperties props;

    public OptionsService(DataGovMandiClient client, MandiProperties props) {
        this.client = client;
        this.props = props;
    }

    @Cacheable(cacheNames = "options", key = "#state + '|' + #district + '|' + #commodity")
    public OptionsResponse discover(String state, String district, String commodity) {
        Map<String, String> f = new LinkedHashMap<>();
        f.put("state", state);
        f.put("district", district);
        f.put("commodity", commodity);
        int pageSize = props.api().pageSize(), cap = props.discovery().maxRecords();
        Set<String> districts = new TreeSet<>(), commodities = new TreeSet<>(), markets = new TreeSet<>();
        Set<VariantOption> variants = new LinkedHashSet<>();
        int offset = 0, scanned = 0;
        boolean truncated = false;
        while (true) {
            DataGovPage page = client.fetch(f, pageSize, offset);
            for (MandiPriceRecord r : page.records()) {
                districts.add(r.district()); commodities.add(r.commodity()); markets.add(r.market());
                variants.add(new VariantOption(r.variety(), r.grade()));
            }
            scanned += page.rawCount();
            offset += pageSize;
            if (page.rawCount() == 0 || offset >= page.total()) break;
            if (scanned >= cap) { truncated = true; break; }
        }
        return new OptionsResponse(List.copyOf(districts), List.copyOf(commodities), List.copyOf(markets),
                List.copyOf(variants), truncated, scanned);
    }
}
