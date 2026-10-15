package com.mandi.dto;

import java.util.List;

public record SyncSummary(int statesProcessed, long received, long invalid, long inserted, long updated,
                          long skipped, List<String> failures) {}
