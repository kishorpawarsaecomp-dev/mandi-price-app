package com.mandi.dto;

import java.util.List;

/** Dropdown values discovered from real API rows. truncated=true when the scan cap was hit. */
public record OptionsResponse(List<String> districts, List<String> commodities, List<String> markets,
                              List<VariantOption> variants, boolean truncated, int scanned) {}
