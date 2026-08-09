package com.aakash.tradenest.order.matching;

import com.aakash.tradenest.order.entity.Order;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class OrderBookManager {

    private final Map<String,OrderBook> books = new ConcurrentHashMap<>();

    public OrderBook getOrCreateBook(String symbol){
        return books.computeIfAbsent(symbol, s-> new OrderBook());
    }
}
