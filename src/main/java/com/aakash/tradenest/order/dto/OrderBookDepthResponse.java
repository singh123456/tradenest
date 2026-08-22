package com.aakash.tradenest.order.dto;

import java.util.List;

public record OrderBookDepthResponse(
        String symbol,
        List<PriceLevel> buyLevels,
        List<PriceLevel> sellLevels
) {
}
