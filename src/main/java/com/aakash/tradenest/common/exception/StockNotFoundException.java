package com.aakash.tradenest.common.exception;

public class StockNotFoundException extends RuntimeException {
    public StockNotFoundException(String symbol) {
        super("No stock found with symbol: " + symbol);
    }
}
