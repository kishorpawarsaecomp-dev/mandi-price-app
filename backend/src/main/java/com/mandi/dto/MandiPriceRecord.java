package com.mandi.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Internal, validated representation of one data.gov.in row. variety/grade are "" when not reported. */
public record MandiPriceRecord(String state, String district, String market, String commodity,
                               String variety, String grade, LocalDate arrivalDate,
                               BigDecimal minPrice, BigDecimal maxPrice, BigDecimal modalPrice) {}
