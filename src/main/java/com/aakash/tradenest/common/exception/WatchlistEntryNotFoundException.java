package com.aakash.tradenest.common.exception;

public class WatchlistEntryNotFoundException extends RuntimeException {
    public WatchlistEntryNotFoundException(String message) {
        super("This Stock is not present in your watchlist");
    }
}
