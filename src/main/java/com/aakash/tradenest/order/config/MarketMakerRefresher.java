package com.aakash.tradenest.order.config;

import com.aakash.tradenest.order.entity.Order;
import com.aakash.tradenest.order.entity.OrderSide;
import com.aakash.tradenest.order.entity.OrderStatus;
import com.aakash.tradenest.order.entity.OrderType;
import com.aakash.tradenest.order.repository.OrderRepository;
import com.aakash.tradenest.order.service.OrderService;
import com.aakash.tradenest.stock.entity.Stock;
import com.aakash.tradenest.stock.repository.StockRepository;
import com.aakash.tradenest.user.entity.User;
import com.aakash.tradenest.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MarketMakerRefresher {

    private static final String MARKET_MAKER_EMAIL = "marketmaker@tradenest.internal";

    private final UserRepository userRepository;
    private final StockRepository stockRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;

    @Scheduled(fixedRate = 20000)
    public void refreshLiquidity(){
        User marketMaker = userRepository.findByEmail(MARKET_MAKER_EMAIL).orElse(null);
        if(marketMaker == null) return;

        for(Stock stock: stockRepository.findAll()){

            List<Order> stale = orderRepository.findByUser_IdAndStock_IdAndStatusIn(
                    marketMaker.getId(), stock.getId(),
                    List.of(OrderStatus.OPEN, OrderStatus.PARTIALLY_FILLED)
            );

            for(Order order: stale){
                try{
                    orderService.cancelOrder(marketMaker.getId(), order.getId());
                }catch (Exception e){
                    System.err.println("Failed to cancel market maker order " + order.getId() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }

            BigDecimal askPrice = stock.getCurrentPrice().multiply(new BigDecimal("1.01"));
            BigDecimal bidPrice = stock.getCurrentPrice().multiply(new BigDecimal("0.99"));

            orderService.placeOrder(marketMaker.getId(), stock.getSymbol(), OrderSide.SELL,
                    OrderType.LIMIT, 5000L, askPrice);
            orderService.placeOrder(marketMaker.getId(), stock.getSymbol(), OrderSide.BUY,
                    OrderType.LIMIT, 5000L, bidPrice);
        }
    }
}
