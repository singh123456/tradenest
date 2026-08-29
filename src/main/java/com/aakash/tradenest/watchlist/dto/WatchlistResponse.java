package com.aakash.tradenest.watchlist.dto;

import java.math.BigDecimal;

public record WatchlistResponse(
        String symbol,
        String name,
        BigDecimal currentPrice
) {
}
