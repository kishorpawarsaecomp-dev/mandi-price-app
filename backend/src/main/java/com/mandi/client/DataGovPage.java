package com.mandi.client;

import com.mandi.dto.MandiPriceRecord;
import java.util.List;

/** One page from data.gov.in: total = rows matching the filters upstream; records = validated rows only. */
public record DataGovPage(long total, List<MandiPriceRecord> records, int rawCount, int invalidCount) {}
