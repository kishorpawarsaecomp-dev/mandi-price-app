package com.mandi.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record HistoricalPriceResponse(
        String state, String district, String commodity, String variety, String grade, String priceType,
        Period period, String aggregationNote,
        List<MarketInfo> availableMarkets, List<String> selectedMarkets, List<VariantOption> availableVariants,
        List<HistoryRecord> records, List<MarketStats> stats, Coverage coverage,
        String source, String disclaimer) {

    public record Period(LocalDate from, LocalDate to) {}
    public record MarketInfo(String market, long records) {}
    public record HistoryRecord(LocalDate date, String market, BigDecimal price) {}
    public record MarketStats(String market, int dataPoints, BigDecimal current, LocalDate currentDate,
                              BigDecimal high, BigDecimal low, BigDecimal average,
                              BigDecimal changePercent, String trend) {}
    public record Coverage(LocalDate firstRecordDate, LocalDate lastRecordDate, long daysWithData,
                           long requestedDays, boolean sufficient, String message) {}
}
