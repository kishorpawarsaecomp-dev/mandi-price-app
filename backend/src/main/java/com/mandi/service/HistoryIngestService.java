package com.mandi.service;

import com.mandi.dto.IngestResult;
import com.mandi.dto.MandiPriceRecord;
import com.mandi.entity.MandiPrice;
import com.mandi.repository.MandiPriceRepository;
import java.math.BigDecimal;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Upsert strategy: natural key = state+district+market+commodity+variety+grade+arrival_date.
 *  - unseen key        -> insert
 *  - same key, changed -> UPDATE the stored row (source corrections win)
 *  - same key, same    -> skipped (no duplicate rows, ever; also enforced by a DB unique constraint)
 */
@Service
public class HistoryIngestService {
    private static final Logger log = LoggerFactory.getLogger(HistoryIngestService.class);
    private final MandiPriceRepository repo;

    public HistoryIngestService(MandiPriceRepository repo) { this.repo = repo; }

    @Transactional
    public IngestResult upsert(Collection<MandiPriceRecord> records) {
        Map<String, MandiPriceRecord> unique = new LinkedHashMap<>();
        int dupInBatch = 0;
        for (MandiPriceRecord r : records) {
            if (unique.put(key(r), r) != null) dupInBatch++;      // last occurrence in the batch wins
        }
        int inserted = 0, updated = 0, skipped = dupInBatch;
        List<MandiPrice> toInsert = new ArrayList<>();
        for (MandiPriceRecord r : unique.values()) {
            Optional<MandiPrice> existing = repo
                    .findByStateAndDistrictAndMarketAndCommodityAndVarietyAndGradeAndArrivalDate(
                            r.state(), r.district(), r.market(), r.commodity(), r.variety(), r.grade(), r.arrivalDate());
            if (existing.isEmpty()) {
                toInsert.add(toEntity(r));
                inserted++;
            } else if (changed(existing.get(), r)) {
                MandiPrice e = existing.get();
                e.setMinPrice(r.minPrice()); e.setMaxPrice(r.maxPrice()); e.setModalPrice(r.modalPrice());
                updated++;
            } else {
                skipped++;
            }
        }
        repo.saveAll(toInsert);
        log.info("Historical records inserted: {}, updated: {}, duplicates skipped: {}", inserted, updated, skipped);
        return new IngestResult(inserted, updated, skipped);
    }

    private static String key(MandiPriceRecord r) {
        return String.join("\u0001", r.state(), r.district(), r.market(), r.commodity(),
                r.variety(), r.grade(), r.arrivalDate().toString());
    }

    private static boolean changed(MandiPrice e, MandiPriceRecord r) {
        return differs(e.getModalPrice(), r.modalPrice()) || differs(e.getMinPrice(), r.minPrice())
                || differs(e.getMaxPrice(), r.maxPrice());
    }

    private static boolean differs(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) return a != b;
        return a.compareTo(b) != 0;
    }

    private static MandiPrice toEntity(MandiPriceRecord r) {
        MandiPrice e = new MandiPrice();
        e.setState(r.state()); e.setDistrict(r.district()); e.setMarket(r.market());
        e.setCommodity(r.commodity()); e.setVariety(r.variety()); e.setGrade(r.grade());
        e.setArrivalDate(r.arrivalDate());
        e.setMinPrice(r.minPrice()); e.setMaxPrice(r.maxPrice()); e.setModalPrice(r.modalPrice());
        return e;
    }
}
