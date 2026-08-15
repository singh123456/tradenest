package com.aakash.tradenest.order.service;

import com.aakash.tradenest.common.exception.InsufficientBalanceException;
import com.aakash.tradenest.common.exception.InsufficientHoldingException;
import com.aakash.tradenest.common.exception.OrderNotFoundException;
import com.aakash.tradenest.common.exception.StockNotFoundException;
import com.aakash.tradenest.order.entity.Order;
import com.aakash.tradenest.order.entity.OrderSide;
import com.aakash.tradenest.order.entity.OrderStatus;
import com.aakash.tradenest.order.entity.OrderType;
import com.aakash.tradenest.order.matching.OrderMatchingEngine;
import com.aakash.tradenest.order.repository.OrderRepository;
import com.aakash.tradenest.portfolio.entity.Holding;
import com.aakash.tradenest.portfolio.service.PortfolioService;
import com.aakash.tradenest.stock.entity.Stock;
import com.aakash.tradenest.stock.repository.StockRepository;
import com.aakash.tradenest.user.entity.User;
import com.aakash.tradenest.user.repository.UserRepository;
import com.aakash.tradenest.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final StockRepository stockRepository;
    private final WalletService walletService;
    private final UserRepository userRepository;
    private final OrderMatchingEngine orderMatchingEngine;
    private final PortfolioService portfolioService;


    @Transactional
    public Order placeOrder(Long userId, String symbol, OrderSide side,
                            OrderType orderType, Long quantity, BigDecimal price){
        if(orderType != OrderType.LIMIT){
            throw new IllegalArgumentException("Only LIMIT orders are supported at this time");
        }

        Stock stock = stockRepository.findBySymbol(symbol)
                .orElseThrow(()-> new StockNotFoundException(symbol));

        if(side == OrderSide.BUY){
            BigDecimal requiredFunds = price.multiply(BigDecimal.valueOf(quantity));
            BigDecimal userBalance = walletService.getBalance(userId);

            if(userBalance.compareTo(requiredFunds) < 0){
                throw new InsufficientBalanceException("Insufficient balance to place this order");
            }
        }else{
            Holding holding = portfolioService.getHoldingForSymbol(userId, stock);

            if(holding.getQuantity() < quantity){
                throw new InsufficientHoldingException("Holding of this stock is not enough to process this order");
            }
        }
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new UsernameNotFoundException("User is not valid user"));
        Order savedOrder = orderRepository.save(Order.builder()
                .status(OrderStatus.OPEN)
                .filledQuantity(0L)
                .user(user)
                .stock(stock)
                .side(side)
                .orderType(orderType)
                .quantity(quantity)
                .price(price)
                .build());


        orderMatchingEngine.processNewOrder(savedOrder);

        return savedOrder;
    }

    public List<Order> getOrdersByUser(Long userId) {

        return orderRepository.findByUserId(userId);
    }

    public Order getOrderById(Long userId, Long id) {
        return orderRepository.findByUserIdAndId(userId,id)
                .orElseThrow(()->new OrderNotFoundException("Order not found with id " +id));
    }
}
