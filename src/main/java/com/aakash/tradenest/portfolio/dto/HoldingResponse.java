package com.aakash.tradenest.portfolio.dto;

import java.math.BigDecimal;

public record HoldingResponse(
        String symbol,
        Long quantity,
        BigDecimal avgBuyPrice,
        BigDecimal currentPrice,
        BigDecimal unrealizedPnL
) {
}
