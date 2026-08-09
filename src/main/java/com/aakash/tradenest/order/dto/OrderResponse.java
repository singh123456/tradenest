package com.aakash.tradenest.order.dto;

import com.aakash.tradenest.order.entity.Order;
import com.aakash.tradenest.order.entity.OrderSide;
import com.aakash.tradenest.order.entity.OrderStatus;
import com.aakash.tradenest.order.entity.OrderType;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderResponse(
        Long id,
        String symbol,
        OrderSide side,
        OrderType orderType,
        Long quantity,
        Long filledQuantity,
        BigDecimal price,
        OrderStatus status,
        Instant createdAt
) {
    public static OrderResponse from(Order order){
        return new OrderResponse(
                order.getId(),
                order.getStock().getSymbol(),
                order.getSide(),
                order.getOrderType(),
                order.getQuantity(),
                order.getFilledQuantity(),
                order.getPrice(),
                order.getStatus(),
                order.getCreatedAt()
        );
    }
}
