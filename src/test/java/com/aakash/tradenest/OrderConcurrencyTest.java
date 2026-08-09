package com.aakash.tradenest;


import com.aakash.tradenest.order.entity.Order;
import com.aakash.tradenest.order.entity.Trade;
import com.aakash.tradenest.order.repository.OrderRepository;
import com.aakash.tradenest.order.repository.TradeRepository;
import com.aakash.tradenest.stock.entity.Stock;
import com.aakash.tradenest.stock.repository.StockRepository;
import com.aakash.tradenest.user.dto.RegisterRequest;
import com.aakash.tradenest.wallet.repository.WalletRepository;
import com.aakash.tradenest.wallet.service.WalletService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.math.BigDecimal;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class OrderConcurrencyTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private WalletService walletService;
    @Autowired
    private StockRepository stockRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private TradeRepository tradeRepository;


    @Test
    void concurrentOrders_shouldMatchExactlyOnce() throws Exception{

        RegisterRequest buyerReq = new RegisterRequest(
                "Buyer",
                "buyer" + System.currentTimeMillis() + "@test.com",
                "pass"
        );

        String buyerResp = mockMvc.perform(post("/api/auth/register")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(buyerReq)))
                .andReturn().getResponse().getContentAsString();

        String buyerToken = objectMapper.readTree(buyerResp).get("token").asText();
        Long buyerId = objectMapper.readTree(buyerResp).get("userId").asLong();


        RegisterRequest sellerReq = new RegisterRequest(
                "Seller",
                "seller" + System.currentTimeMillis() + "@test.com",
                "pass"
        );

        String sellerResp = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(sellerReq)))
                .andReturn().getResponse().getContentAsString();

        String sellerToken = objectMapper.readTree(sellerResp).get("token").asText();
        Long sellerId = objectMapper.readTree(sellerResp).get("userId").asLong();

        walletService.credit(buyerId,new BigDecimal("100000"));
        walletService.credit(sellerId,new BigDecimal("1000"));

        stockRepository.save(Stock.builder()
                .symbol("CONC_STOCK")
                .name("Concurrency Inc")
                .currentPrice(new BigDecimal("100"))
                .build());

        String buyJson = """
            {
            "symbol":"CONC_STOCK",
            "side":"BUY",
            "orderType":"LIMIT",
            "quantity":10,
            "price":100
            }
            """;

        String sellJson = """
            {
            "symbol":"CONC_STOCK",
            "side":"SELL",
            "orderType":"LIMIT",
            "quantity":10,
            "price":100
            }
            """;

        ExecutorService executor = Executors.newFixedThreadPool(2);

        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);


        Runnable buyTask = ()->{
            try{
                readyLatch.countDown();;
                startLatch.await();

                mockMvc.perform(post("/api/orders")
                        .header("Authorization","Bearer "+ buyerToken)
                        .contentType("application/json")
                        .content(buyJson)).andReturn();
            }catch (Exception e){
                throw new RuntimeException(e);
            }finally {
                doneLatch.countDown();
            }
        };

        Runnable sellTask = () ->{
            try{
                readyLatch.countDown();
                startLatch.await();

                mockMvc.perform(post("/api/orders")
                        .header("Authorization","Bearer "+ sellerToken)
                        .contentType("application/json")
                        .content(sellJson)).andReturn();

            }catch (Exception e){
                throw new RuntimeException(e);
            }finally {
                doneLatch.countDown();
            }
        };

        executor.submit(buyTask);
        executor.submit(sellTask);

        readyLatch.await();

        startLatch.countDown();

        doneLatch.await();

        executor.shutdown();

        List<Order> orders = orderRepository.findAll();
        assertThat(orders).hasSize(2);

        orders.forEach(order->{
            assertThat(order.getFilledQuantity()).isEqualTo(order.getQuantity());
        });

        List<Trade> trades = tradeRepository.findAll();

        assertThat(trades).hasSize(1);

        Trade trade = trades.get(0);
        assertThat(trade.getQuantity()).isEqualTo(10L);
        assertThat(trade.getPrice()).isEqualTo(new BigDecimal(100));
    }
}
