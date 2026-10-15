package com.mandi.service;

import com.mandi.config.MandiProperties;
import com.mandi.dto.HistoricalPriceResponse;
import com.mandi.dto.HistoricalPriceResponse.*;
import com.mandi.dto.PriceType;
import com.mandi.dto.VariantOption;
import com.mandi.entity.MandiPrice;
import com.mandi.exception.BadRequestException;
import com.mandi.repository.MandiPriceRepository;
import com.mandi.repository.MandiPriceRepository.CoverageView;
import com.mandi.repository.MandiPriceRepository.MarketCountView;
import com.mandi.repository.MandiPriceRepository.VariantCount;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HistoricalPriceService {
    static final LocalDate EPOCH = LocalDate.of(2000, 1, 1);
    static final int MAX_MARKETS = 8, DEFAULT_MARKETS = 5, MAX_RANGE_DAYS = 3660;
    static final String SOURCE = "data.gov.in (accumulated by this application's daily sync)";
    static final String DISCLAIMER = "Historical trend indicator based on reported prices only. "
            + "It is not a forecast or a guarantee of future prices.";

    private final MandiPriceRepository repo;
    private final MandiProperties props;

    public HistoricalPriceService(MandiPriceRepository repo, MandiProperties props) {
        this.repo = repo;
        this.props = props;
    }

    public record Request(String state, String district, String commodity, String market, List<String> markets,
                          String variety, String grade, LocalDate from, LocalDate to, PriceType type) {}

    @Transactional(readOnly = true)
    public HistoricalPriceResponse history(Request q) {
        LocalDate end = q.to() != null ? q.to() : LocalDate.now();
        LocalDate start = q.from() != null ? q.from() : end.minusDays(29);
        if (start.isAfter(end)) throw new BadRequestException("'from' must not be after 'to'.");
        long requestedDays = ChronoUnit.DAYS.between(start, end) + 1;
        if (requestedDays > MAX_RANGE_DAYS) throw new BadRequestException("Date range is too large.");
        PriceType type = q.type() == null ? PriceType.MODAL : q.type();
        Period period = new Period(start, end);

        // 1. Pick ONE variety/grade series. Different varieties/grades are never merged.
        List<VariantCount> variants = repo.variantCounts(q.state(), q.district(), q.commodity(), EPOCH, end);
        List<VariantOption> options = variants.stream()
                .map(v -> new VariantOption(v.getVariety(), v.getGrade())).toList();
        VariantCount chosen = variants.stream()
                .filter(v -> q.variety() == null || v.getVariety().equalsIgnoreCase(q.variety()))
                .filter(v -> q.grade() == null || v.getGrade().equalsIgnoreCase(q.grade()))
                .findFirst().orElse(null);
        if (chosen == null) {
            return empty(q, type, period, options, requestedDays,
                    "No stored history for this selection yet. History builds up as the daily sync collects data.");
        }
        String variety = chosen.getVariety(), grade = chosen.getGrade();
        String note = (q.variety() != null || variants.size() == 1)
                ? "Showing variety \"" + label(variety) + "\", grade \"" + label(grade) + "\"."
                : "This commodity has several varieties/grades; they are never combined. Showing the most frequently "
                  + "reported one: \"" + label(variety) + "\", grade \"" + label(grade) + "\".";

        // 2. Markets: explicit market > explicit list > top N by number of reported days.
        List<MarketCountView> mc = repo.marketCounts(q.state(), q.district(), q.commodity(), variety, grade, start, end);
        List<MarketInfo> available = mc.stream().map(m -> new MarketInfo(m.getMarket(), m.getCnt())).toList();
        List<String> selected;
        if (q.market() != null) selected = List.of(q.market());
        else if (q.markets() != null && !q.markets().isEmpty()) selected = q.markets().stream().distinct().limit(MAX_MARKETS).toList();
        else selected = available.stream().limit(DEFAULT_MARKETS).map(MarketInfo::market).toList();

        // 3. Real records only, only the selected markets and period.
        List<MandiPrice> rows = selected.isEmpty() ? List.of()
                : repo.findSeries(q.state(), q.district(), q.commodity(), variety, grade, start, end, selected);
        List<HistoryRecord> records = new ArrayList<>();
        for (MandiPrice p : rows) {
            BigDecimal price = pick(p, type);
            if (price != null) records.add(new HistoryRecord(p.getArrivalDate(), p.getMarket(), price));
        }

        // 4. Per-market statistics (markets are never averaged together).
        Map<String, List<PriceStatistics.Point>> byMarket = new LinkedHashMap<>();
        selected.forEach(m -> byMarket.put(m, new ArrayList<>()));
        records.forEach(r -> byMarket.computeIfAbsent(r.market(), k -> new ArrayList<>())
                .add(new PriceStatistics.Point(r.date(), r.price())));
        List<MarketStats> stats = new ArrayList<>();
        byMarket.forEach((market, pts) -> {
            PriceStatistics.Result r = PriceStatistics.compute(pts, props.trend().thresholdPercent(), props.trend().minPoints());
            if (r != null) stats.add(new MarketStats(market, r.points(), r.current(), r.currentDate(), r.high(),
                    r.low(), r.average(), r.changePercent(), r.trend().name()));
        });

        // 5. Does stored history really cover the requested period?
        CoverageView cov = repo.coverage(q.state(), q.district(), q.commodity(), variety, grade, EPOCH, end);
        long daysWithData = records.stream().map(HistoryRecord::date).distinct().count();
        Coverage coverage = coverage(cov.getFirstDate(), cov.getLastDate(), daysWithData, requestedDays, start);

        return new HistoricalPriceResponse(q.state(), q.district(), q.commodity(), variety, grade, type.name().toLowerCase(),
                period, note, available, selected, options, records, stats, coverage, SOURCE, DISCLAIMER);
    }

    static Coverage coverage(LocalDate first, LocalDate last, long daysWithData, long requestedDays, LocalDate start) {
        if (first == null) {
            return new Coverage(null, null, 0, requestedDays, false, "No history stored for this selection yet.");
        }
        long tolerance = Math.max(3, requestedDays / 10);
        boolean sufficient = !first.isAfter(start.plusDays(tolerance));
        long storedSpan = ChronoUnit.DAYS.between(first, last) + 1;
        String msg = sufficient
                ? daysWithData + " of " + requestedDays + " calendar days in this period have a reported record. "
                  + "Days without a record are left blank; no values are estimated."
                : "Not enough history yet for a " + requestedDays + "-day view: records exist only from " + first
                  + " (about " + storedSpan + " day(s) collected). The chart shows only what has been stored.";
        return new Coverage(first, last, daysWithData, requestedDays, sufficient, msg);
    }

    private HistoricalPriceResponse empty(Request q, PriceType type, Period period, List<VariantOption> options,
                                          long requestedDays, String msg) {
        Coverage c = new Coverage(null, null, 0, requestedDays, false, msg);
        return new HistoricalPriceResponse(q.state(), q.district(), q.commodity(), q.variety(), q.grade(),
                type.name().toLowerCase(), period, msg, List.of(), List.of(), options, List.of(), List.of(), c,
                SOURCE, DISCLAIMER);
    }

    private static BigDecimal pick(MandiPrice p, PriceType t) {
        return switch (t) { case MODAL -> p.getModalPrice(); case MIN -> p.getMinPrice(); case MAX -> p.getMaxPrice(); };
    }

    private static String label(String s) { return s == null || s.isEmpty() ? "not specified" : s; }
}
