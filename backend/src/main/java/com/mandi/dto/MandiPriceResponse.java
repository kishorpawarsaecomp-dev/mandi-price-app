package com.mandi.dto;

import java.time.Instant;
import java.util.List;

public record MandiPriceResponse(long total, int limit, int offset, List<MandiPriceRecord> records,
                                 String source, Instant fetchedAt) {}
