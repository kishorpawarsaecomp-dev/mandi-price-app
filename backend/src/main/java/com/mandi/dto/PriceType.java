package com.mandi.dto;

import com.mandi.exception.BadRequestException;

public enum PriceType {
    MODAL, MIN, MAX;

    public static PriceType parse(String s) {
        if (s == null || s.isBlank()) return MODAL;
        try { return valueOf(s.trim().toUpperCase()); }
        catch (IllegalArgumentException e) { throw new BadRequestException("priceType must be modal, min or max."); }
    }
}
