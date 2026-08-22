package com.aakash.tradenest.order.dto;

import java.math.BigDecimal;

public record PriceLevel(BigDecimal price, Long totalQuantity, int orderCount) {
}
