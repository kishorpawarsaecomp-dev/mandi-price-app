package com.mandi.service;

import com.mandi.client.DataGovMandiClient;
import com.mandi.client.DataGovPage;
import com.mandi.dto.MandiPriceResponse;
import com.mandi.dto.PriceQuery;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class MandiPriceService {
    private static final Logger log = LoggerFactory.getLogger(MandiPriceService.class);
    public static final String SOURCE = "data.gov.in";

    private final DataGovMandiClient client;
    private final HistoryIngestService ingest;

    public MandiPriceService(DataGovMandiClient client, HistoryIngestService ingest) {
        this.client = client;
        this.ingest = ingest;
    }

    /** Cached per normalized query: 100 identical requests => 1 upstream call per TTL. Errors are not cached. */
    @Cacheable(cacheNames = "prices", key = "#query")
    public MandiPriceResponse getPrices(PriceQuery query) {
        Map<String, String> f = new LinkedHashMap<>();
        f.put("state", query.state());
        f.put("district", query.district());
        f.put("market", query.market());
        f.put("commodity", query.commodity());
        f.put("variety", query.variety());
        f.put("grade", query.grade());
        DataGovPage page = client.fetch(f, query.limit(), query.offset());

        // Every real record we see also feeds the history database (never blocks the response).
        try {
            if (!page.records().isEmpty()) ingest.upsert(page.records());
        } catch (Exception e) {
            log.warn("Could not store fetched records in history: {}", e.getMessage());
        }
        return new MandiPriceResponse(page.total(), query.limit(), query.offset(), page.records(), SOURCE, Instant.now());
    }
}
