package com.aakash.tradenest.wallet.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record AddMoneyRequest(
        @NotNull @Positive BigDecimal amount
        ) {
}
