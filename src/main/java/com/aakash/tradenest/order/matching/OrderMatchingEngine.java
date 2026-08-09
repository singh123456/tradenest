package com.aakash.tradenest.order.matching;

import com.aakash.tradenest.order.entity.Order;
import com.aakash.tradenest.order.entity.OrderSide;
import com.aakash.tradenest.order.entity.OrderStatus;
import com.aakash.tradenest.order.entity.Trade;
import com.aakash.tradenest.order.repository.OrderRepository;
import com.aakash.tradenest.order.repository.TradeRepository;
import com.aakash.tradenest.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class OrderMatchingEngine {

    private final OrderBookManager orderBookManager;
    private final OrderRepository orderRepository;
    private final TradeRepository tradeRepository;
    private final WalletService walletService;

    public void processNewOrder(Order incomingOrder){
        OrderBook orderBook = orderBookManager.getOrCreateBook(incomingOrder.getStock().getSymbol());
        orderBook.lock();
        try {
            while (getRemainingQuantity(incomingOrder) > 0) {
                Order bestOpposite;
                if (incomingOrder.getSide() == OrderSide.BUY) {
                    bestOpposite = orderBook.peekBestSell();
                    if (bestOpposite == null || !canMatch(incomingOrder, bestOpposite)) {
                        break;
                    }
                } else {
                    bestOpposite = orderBook.peekBestBuy();
                    if (bestOpposite == null || !canMatch(incomingOrder, bestOpposite)) {
                        break;
                    }
                }
                Long incomingRemaining = getRemainingQuantity(incomingOrder);
                Long restingRemaining = getRemainingQuantity(bestOpposite);

                Long matchQty = Math.min(incomingRemaining, restingRemaining);

                BigDecimal matchPrice = bestOpposite.getPrice();

                if (incomingOrder.getSide() == OrderSide.BUY) {
                    executeTrade(incomingOrder, bestOpposite, matchPrice, matchQty);
                } else {
                    executeTrade(bestOpposite, incomingOrder, matchPrice, matchQty);
                }

                if (getRemainingQuantity(bestOpposite) == 0) {
                    if (incomingOrder.getSide() == OrderSide.BUY) {
                        orderBook.pollBestSell();
                    } else {
                        orderBook.pollBestBuy();
                    }
                }
            }

            if (getRemainingQuantity(incomingOrder) > 0) {
                orderBook.addOrder(incomingOrder);

                if (incomingOrder.getFilledQuantity() > 0) {
                    incomingOrder.setStatus(OrderStatus.PARTIALLY_FILLED);
                } else {
                    incomingOrder.setStatus(OrderStatus.OPEN);
                }
            } else {
                incomingOrder.setStatus(OrderStatus.FILLED);
            }

            orderRepository.save(incomingOrder);

        }finally {
            orderBook.unlock();
        }
    }

    private Long getRemainingQuantity(Order order){
        Long filled =  order.getFilledQuantity() == null ? 0L: order.getFilledQuantity();
        return order.getQuantity() - filled;
    }

    private boolean canMatch(Order incoming, Order restingOpposite){

        if(incoming.getSide() == OrderSide.BUY){
            return incoming.getPrice().compareTo(restingOpposite.getPrice()) >= 0;
        }else{
            return incoming.getPrice().compareTo(restingOpposite.getPrice()) <= 0;
        }
    }


    private void executeTrade(Order buyOrder, Order sellOrder, BigDecimal matchPrice, Long matchQuantity){

        BigDecimal tradeValue = matchPrice.multiply(BigDecimal.valueOf(matchQuantity));

        Trade trade = Trade.builder()
                .buyOrder(buyOrder)
                .sellOrder(sellOrder)
                .stock(buyOrder.getStock())
                .quantity(matchQuantity)
                .price(matchPrice)
                .build();
        tradeRepository.save(trade);

        buyOrder.setFilledQuantity(buyOrder.getFilledQuantity() + matchQuantity);

        sellOrder.setFilledQuantity(sellOrder.getFilledQuantity() + matchQuantity);

        updateOrderStatus(buyOrder);
        updateOrderStatus(sellOrder);



        walletService.debit(buyOrder.getUser().getId(), tradeValue);
        walletService.credit(sellOrder.getUser().getId(), tradeValue);

        orderRepository.save(buyOrder);
        orderRepository.save(sellOrder);
    }


    private void updateOrderStatus(Order order){
        if(order.getFilledQuantity().equals(order.getQuantity())){
            order.setStatus(OrderStatus.FILLED);
        }else{
            order.setStatus(OrderStatus.PARTIALLY_FILLED);
        }
    }

}
