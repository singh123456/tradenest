package com.aakash.tradenest.order.matching;

import com.aakash.tradenest.order.entity.Order;
import com.aakash.tradenest.order.entity.OrderSide;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.concurrent.locks.ReentrantLock;

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

    public void lock(){
        lock.lock();
    }

    public void unlock(){
        lock.unlock();
    }
}
