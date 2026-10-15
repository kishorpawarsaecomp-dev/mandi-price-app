package com.mandi.dto;

/** Normalized query; also the cache key (records have value equality). */
public record PriceQuery(String state, String district, String market, String commodity,
                         String variety, String grade, int limit, int offset) {}
