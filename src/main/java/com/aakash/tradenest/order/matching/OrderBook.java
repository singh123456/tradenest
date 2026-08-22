package com.aakash.tradenest.order.matching;

import com.aakash.tradenest.order.dto.PriceLevel;
import com.aakash.tradenest.order.entity.Order;
import com.aakash.tradenest.order.entity.OrderSide;
import org.antlr.v4.runtime.tree.Tree;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class OrderBook {

    private final PriorityQueue<Order> buyOrders;
    private final PriorityQueue<Order> sellOrders;
    private final ReentrantLock lock = new ReentrantLock();

    public OrderBook(){
        Comparator<Order> buyComparator = Comparator
                .comparing(Order::getPrice, Comparator.reverseOrder())
                .thenComparing(Order::getCreatedAt);

        Comparator<Order> sellComparator = Comparator
                .comparing(Order::getPrice)
                .thenComparing(Order::getCreatedAt);

        this.buyOrders = new PriorityQueue<>(buyComparator);
        this.sellOrders = new PriorityQueue<>(sellComparator);

    }

    public void addOrder(Order order){
        lock.lock();
        try{
            if(order.getSide() == OrderSide.BUY){
                buyOrders.add(order);
            }else{
                sellOrders.add(order);
            }
        }finally {
            lock.unlock();
        }
    }

    public Order peekBestBuy(){
        lock.lock();
        try{
            return buyOrders.peek();
        }finally {
            lock.unlock();
        }
    }

    public Order peekBestSell(){
        lock.lock();
        try {
            return sellOrders.peek();
        }finally {
            lock.unlock();
        }
    }

    public Order pollBestBuy(){
        lock.lock();
        try{
            return buyOrders.poll();
        }finally {
            lock.unlock();
        }
    }

    public Order pollBestSell(){
        lock.lock();
        try{
            return sellOrders.poll();
        }finally {
            lock.unlock();
        }
    }

    public void removeOrder(Order order){
        lock.lock();
        try {


            if (order.getSide() == OrderSide.SELL) {
                sellOrders.remove(order);
            } else {
                buyOrders.remove(order);
            }
        }finally {
            lock.unlock();
        }
    }

    public List<PriceLevel> getBuyDepth(){
        lock.lock();
        try{
            List<Order> orders = new ArrayList<>(buyOrders);
            Map<BigDecimal, List<Order>> grouped = orders.stream()
                    .collect(Collectors.groupingBy(Order::getPrice, TreeMap::new, Collectors.toList()));
            List<PriceLevel> result = new ArrayList<>();
            for(Map.Entry<BigDecimal, List<Order>> entry: grouped.entrySet()){

                BigDecimal price = entry.getKey();
                List<Order> ordersAtPrice = entry.getValue();

                Long totalQuantity = ordersAtPrice.stream()
                        .mapToLong(order->{
                            Long filledQuantity = order.getFilledQuantity() == null
                                    ?0L
                                    : order.getFilledQuantity();
                            return order.getQuantity() - filledQuantity;
                        })
                        .sum();

                int orderCount = (int) ordersAtPrice.stream()
                        .filter(order->{
                            Long filledQuantity = order.getFilledQuantity() == null
                                    ?0L
                                    : order.getFilledQuantity();
                            return order.getQuantity() - filledQuantity > 0;
                        })
                        .count();

                if(totalQuantity > 0){
                    result.add(
                            new PriceLevel(
                                    price,
                                    totalQuantity,
                                    orderCount
                            )
                    );
                }
            }
            result.sort(
                    Comparator.comparing(PriceLevel::price).reversed()
            );
            return result;
        }finally {
            lock.unlock();
        }
    }

    public List<PriceLevel> getSellDepth(){
        lock.lock();
        try{
            List<Order> orders = new ArrayList<>(sellOrders);
            Map<BigDecimal, List<Order>> grouped = orders.stream()
                    .collect(Collectors.groupingBy(Order::getPrice));
            List<PriceLevel> result = new ArrayList<>();
            for(Map.Entry<BigDecimal, List<Order>> entry: grouped.entrySet()){

                BigDecimal price = entry.getKey();
                List<Order> ordersAtPrice = entry.getValue();

                Long totalQuantity = ordersAtPrice.stream()
                        .mapToLong(order->{
                            Long filledQuantity = order.getFilledQuantity() == null
                                    ?0L
                                    : order.getFilledQuantity();
                            return order.getQuantity() - filledQuantity;
                        })
                        .sum();

                int orderCount = (int) ordersAtPrice.stream()
                        .filter(order->{
                            Long filledQuantity = order.getFilledQuantity() == null
                                    ?0L
                                    : order.getFilledQuantity();
                            return order.getQuantity() - filledQuantity > 0;
                        })
                        .count();

                if(totalQuantity > 0){
                    result.add(
                            new PriceLevel(
                                    price,
                                    totalQuantity,
                                    orderCount
                            )
                    );
                }
            }
            result.sort(
                    Comparator.comparing(PriceLevel::price)
            );
            return result;
        }finally {
            lock.unlock();
        }
    }

    public void lock(){
        lock.lock();
    }

    public void unlock(){
        lock.unlock();
    }
}
