package com.mandi.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ComparisonResponse(String state, String district, String commodity, String variety, String grade,
                                 LocalDate latestDate, List<Row> rows, List<VariantOption> availableVariants,
                                 String source) {
    public record Row(String market, BigDecimal modalPrice, BigDecimal minPrice, BigDecimal maxPrice,
                      LocalDate arrivalDate) {}
}
