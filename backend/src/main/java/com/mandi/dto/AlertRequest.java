package com.mandi.dto;

import com.mandi.entity.PriceAlert;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record AlertRequest(@NotBlank String state, @NotBlank String district, @NotBlank String commodity,
                           String market, @NotNull PriceAlert.Direction direction,
                           @NotNull @Positive BigDecimal threshold) {}
