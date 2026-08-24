package com.aakash.tradenest.order.config;

import com.aakash.tradenest.order.entity.OrderSide;
import com.aakash.tradenest.order.entity.OrderType;
import com.aakash.tradenest.order.service.OrderService;
import com.aakash.tradenest.portfolio.entity.Holding;
import com.aakash.tradenest.portfolio.repository.HoldingRepository;
import com.aakash.tradenest.stock.entity.Stock;
import com.aakash.tradenest.stock.repository.StockRepository;
import com.aakash.tradenest.user.entity.Role;
import com.aakash.tradenest.user.entity.User;
import com.aakash.tradenest.user.repository.UserRepository;
import com.aakash.tradenest.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@Order(1)
@RequiredArgsConstructor
public class MarketMakerSeeder implements CommandLineRunner {

    private static final String MARKET_MAKER_EMAIL = "marketmaker@tradenest.internal";

    private final UserRepository userRepository;
    private final WalletService walletService;
    private final HoldingRepository holdingRepository;
    private final StockRepository stockRepository;
    private final OrderService orderService;
    private final PasswordEncoder passwordEncoder;


    @Override
    public void run(String... args) throws Exception {
        if(userRepository.existsByEmail(MARKET_MAKER_EMAIL)){
            return;
        }

        User marketMaker = userRepository.save(User.builder()
                .name("Market Maker")
                .email(MARKET_MAKER_EMAIL)
                .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                .role(Role.MARKET_MAKER)
                .build());

        walletService.createWallet(marketMaker);
        walletService.credit(marketMaker.getId(), new BigDecimal("100000000"));

        for(Stock stock: stockRepository.findAll()){

            holdingRepository.save(Holding.builder()
                    .user(marketMaker)
                    .stock(stock)
                    .quantity(100000L)
                    .avgBuyPrice(stock.getCurrentPrice())
                    .build()
            );

            BigDecimal askPrice = stock.getCurrentPrice().multiply(new BigDecimal("1.01"));
            BigDecimal bidPrice = stock.getCurrentPrice().multiply(new BigDecimal("0.99"));

            orderService.placeOrder(marketMaker.getId(), stock.getSymbol(),
                    OrderSide.SELL, OrderType.LIMIT, 5000L, askPrice);
            orderService.placeOrder(marketMaker.getId(), stock.getSymbol(), OrderSide.BUY,
                    OrderType.LIMIT, 5000L, bidPrice);

        }
    }
}
