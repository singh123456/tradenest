package com.aakash.tradenest.order.dto;

import com.aakash.tradenest.order.entity.OrderSide;
import com.aakash.tradenest.order.entity.OrderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PlaceOrderRequest(
        @NotBlank String symbol,
        @NotNull OrderSide side,
        @NotNull OrderType orderType,
        @NotNull @Positive Long quantity,
        @NotNull @Positive BigDecimal price
        )
{}
