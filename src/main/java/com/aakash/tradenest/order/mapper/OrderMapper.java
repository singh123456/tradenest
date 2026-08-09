package com.aakash.tradenest.order.mapper;

import com.aakash.tradenest.order.dto.OrderResponse;
import com.aakash.tradenest.order.entity.Order;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {

    public OrderResponse toResponse(Order order){
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
