package com.aakash.tradenest;

import com.aakash.tradenest.order.entity.Order;
import com.aakash.tradenest.order.entity.OrderStatus;
import com.aakash.tradenest.order.entity.Trade;
import com.aakash.tradenest.order.matching.OrderMatchingEngine;
import com.aakash.tradenest.order.repository.OrderRepository;
import com.aakash.tradenest.order.repository.TradeRepository;
import com.aakash.tradenest.portfolio.entity.Holding;
import com.aakash.tradenest.portfolio.repository.HoldingRepository;
import com.aakash.tradenest.stock.entity.Stock;
import com.aakash.tradenest.stock.repository.StockRepository;
import com.aakash.tradenest.user.dto.RegisterRequest;
import com.aakash.tradenest.user.entity.User;
import com.aakash.tradenest.user.repository.UserRepository;
import com.aakash.tradenest.wallet.service.WalletService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class PortfolioIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WalletService walletService;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private OrderMatchingEngine orderMatchingEngine;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TradeRepository tradeRepository;

    @Autowired
    private HoldingRepository holdingRepository;

    @Test
    void buyOrder_createdHoldingAfterTrade() throws Exception{

        RegisterRequest buyerReq = new RegisterRequest(
                "Buyer",
                "buyer" + System.currentTimeMillis() + "@example.com",
                "SecurePass123"
        );

        String buyerResponse = mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(buyerReq))

                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode buyerJson = objectMapper.readTree(buyerResponse);

        String buyerToken = buyerJson.get("token").asText();
        Long buyerId = buyerJson.get("userId").asLong();


        RegisterRequest sellerReq = new RegisterRequest(
                "Seller",
                "seller" + System.currentTimeMillis() + "@example.com", "SecurePass123"
        );

        String sellerResponse = mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(sellerReq))

                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode sellerJson = objectMapper.readTree(sellerResponse);

        String sellerToken = sellerJson.get("token").asText();
        Long sellerId = sellerJson.get("userId").asLong();


        Stock stock = stockRepository.save(
                Stock.builder()
                        .symbol("TESTSTOCK")
                        .name("Test Stock Inc")
                        .currentPrice(new BigDecimal("100"))
                        .build()
        );

        walletService.credit(
                buyerId,
                new BigDecimal("100000")
        );

        User seller = userRepository.findById(sellerId)
                .orElseThrow();

        holdingRepository.save(
                Holding.builder()
                        .user(seller)
                        .stock(stock)
                        .quantity(10L)
                        .avgBuyPrice(new BigDecimal("90"))
                        .build()
        );


        String sellJson = """
                {
                   "symbol": "TESTSTOCK",
                   "side": "SELL",
                   "orderType": "LIMIT",
                   "quantity": 10,
                   "price": 100
                
                }
                """;

        mockMvc.perform(
                        post("/api/orders")
                                .header(
                                        "Authorization",
                                        "Bearer " + sellerToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(sellJson)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"));

        String buyJson = """
                {
                    "symbol": "TESTSTOCK",
                    "side": "BUY",
                    "orderType": "LIMIT",
                    "quantity": 10,
                    "price": 100
                
                }
                """;

        mockMvc.perform(
                        post("/api/orders")
                                .header(
                                        "Authorization",
                                        "Bearer " + buyerToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(buyJson)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("FILLED"));


        List<Order> orders = orderRepository.findAll();

        assertThat(orders).hasSize(2);

        for(Order order: orders){

            assertThat(order.getStatus())
                    .isEqualTo(OrderStatus.FILLED);

            assertThat(order.getFilledQuantity())
                    .isEqualTo(order.getQuantity());
        }

        List<Trade> trades = tradeRepository.findAll();

        assertThat(trades).hasSize(1);

        Trade trade = trades.get(0);

        assertThat(trade.getQuantity())
                .isEqualTo(10L);

        assertThat(trade.getPrice())
                .isEqualByComparingTo("100");

        assertThat(trade.getStock().getSymbol())
                .isEqualTo("TESTSTOCK");

        Optional<Holding> buyerHolding = holdingRepository.findByUser_IdAndStock_Id(
                buyerId,
                stock.getId()
        );

        assertThat(buyerHolding).isPresent();

        assertThat(buyerHolding.get().getQuantity())
                .isEqualTo(10L);

        assertThat(buyerHolding.get().getAvgBuyPrice())
                .isEqualByComparingTo("100");


        Optional<Holding> sellerHolding =
                holdingRepository.findByUser_IdAndStock_Id(sellerId,stock.getId());

        assertThat(sellerHolding).isEmpty();

    }


    @Test
    void secondBuyOrder_updatesAvgTrade() throws Exception{

        RegisterRequest buyerReq = new RegisterRequest(
                "Buyer",
                "buyer" + System.currentTimeMillis() + "@example.com",
                "SecurePass123"
        );
        String buyerResponse = mockMvc.perform(
                post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buyerReq))
        )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode buyerJson = objectMapper.readTree(buyerResponse);

        String buyerToken = buyerJson.get("token").asText();
        Long buyerId = buyerJson.get("userId").asLong();

        RegisterRequest sellerReq = new RegisterRequest(
                "Seller One",
                "seller1" + System.currentTimeMillis() + "@example.com",
                "SecurePass123"
        );

        String sellerResponse = mockMvc.perform(
                post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sellerReq))
        )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode seller1Json = objectMapper.readTree(sellerResponse);

        String seller1Token = seller1Json.get("token").asText();
        Long seller1Id = seller1Json.get("userId").asLong();


        RegisterRequest seller2Req = new RegisterRequest(
                "Seller Two",
                "seller2" + System.currentTimeMillis() + "@example.com",
                "SecurePass123"
        );

        String seller2Response = mockMvc.perform(
                post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(seller2Req))
        )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode seller2Json = objectMapper.readTree(seller2Response);

        String seller2Token = seller2Json.get("token").asText();
        Long seller2Id = seller2Json.get("userId").asLong();

        Stock stock = stockRepository.save(
                Stock.builder()
                        .symbol("TESTSTOCK")
                        .name("Test Stock")
                        .currentPrice(new BigDecimal("120"))
                        .build()
        );

        walletService.credit(
                buyerId, new BigDecimal("100000")
        );

        User seller1 = userRepository.findById(seller1Id)
                .orElseThrow();

        holdingRepository.save(
                Holding.builder()
                        .user(seller1)
                        .stock(stock)
                        .quantity(10L)
                        .avgBuyPrice(new BigDecimal("90"))
                        .build()
        );

        User seller2 = userRepository.findById(seller2Id)
                .orElseThrow();

        holdingRepository.save(
                Holding.builder()
                        .user(seller2)
                        .stock(stock)
                        .quantity(10L)
                        .avgBuyPrice(new BigDecimal("110"))
                        .build()
        );

String sell1Json = """
        {
            "symbol": "TESTSTOCK",
            "side": "SELL",
            "orderType": "LIMIT",
            "quantity": 10,
            "price": 100
        }
        
        """;

mockMvc.perform(
        post("/api/orders")
                .header("Authorization",
                        "Bearer "+ seller1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(sell1Json)
)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("OPEN"));

String buy1Json = """
        {
            "symbol": "TESTSTOCK",
            "side": "BUY",
            "orderType": "LIMIT",
            "quantity": 10,
            "price": 100
        }
        
        """;

mockMvc.perform(
        post("/api/orders")
                .header(
                        "Authorization",
                        "Bearer "+ buyerToken
                )
                .contentType(MediaType.APPLICATION_JSON)
                .content(buy1Json)
)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("FILLED"));


        Holding buyerHoldingAfterFirstTrade =
                holdingRepository.findByUser_IdAndStock_Id(
                        buyerId,
                        stock.getId()
                ).orElseThrow();

        assertThat(buyerHoldingAfterFirstTrade.getQuantity())
                .isEqualTo(10L);

        assertThat(buyerHoldingAfterFirstTrade.getAvgBuyPrice())
                .isEqualByComparingTo("100");


        String sell2Json = """
                {
                    "symbol": "TESTSTOCK",
                    "side": "SELL",
                    "orderType": "LIMIT",
                    "quantity": 10,
                    "price": 120
                
                }
                
                """;

        mockMvc.perform(
                post("/api/orders")
                        .header(
                                "Authorization",
                                "Bearer "+ seller2Token
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sell2Json)
        )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"));

        String buy2Json = """
            {
                "symbol": "TESTSTOCK",
                "side": "BUY",
                "orderType": "LIMIT",
                "quantity": 10,
                "price": 120
            }
            """;

        mockMvc.perform(
                        post("/api/orders")
                                .header(
                                        "Authorization",
                                        "Bearer " + buyerToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(buy2Json)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("FILLED"));

        Holding finalHolding =
                holdingRepository.findByUser_IdAndStock_Id(
                        buyerId,
                        stock.getId()
                ).orElseThrow();

        assertThat(finalHolding.getQuantity())
                .isEqualTo(20L);

        assertThat(finalHolding.getAvgBuyPrice())
                .isEqualByComparingTo("110");
    }

    @Test
    void sellOrder_decreasesHoldingQuantity() throws Exception{

        RegisterRequest seller1Req = new RegisterRequest(
                "Seller One",
                "seller1" + System.currentTimeMillis() + "@example.com",
                "SecurePass123"
        );

        String seller1Response = mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(seller1Req))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode seller1Json = objectMapper.readTree(seller1Response);

        String seller1Token = seller1Json.get("token").asText();
        Long seller1Id = seller1Json.get("userId").asLong();

        RegisterRequest seller2Req = new RegisterRequest(
                "Seller Two",
                "seller2" + System.currentTimeMillis() + "@example.com",
                "SecurePass123"
        );

        String seller2Response = mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(seller2Req))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode seller2Json = objectMapper.readTree(seller2Response);

        String seller2Token = seller2Json.get("token").asText();
        Long seller2Id = seller2Json.get("userId").asLong();

        RegisterRequest buyerReq = new RegisterRequest(
                "Buyer",
                "buyer" + System.currentTimeMillis() + "@example.com",
                "SecurePass123"
        );

        String buyerResponse = mockMvc.perform(
                post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buyerReq))
        )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode buyerJson = objectMapper.readTree(buyerResponse);

        String buyerToken = buyerJson.get("token").asText();
        Long buyerId = buyerJson.get("userId").asLong();

        Stock stock = stockRepository.save(
                Stock.builder()
                        .symbol("TESTSTOCK")
                        .name("Test Stock")
                        .currentPrice(new BigDecimal("100"))
                        .build()
        );

        walletService.credit(
                buyerId,
                new BigDecimal("100000")
        );

        User seller1 = userRepository.findById(seller1Id)
                .orElseThrow();

        holdingRepository.save(
                Holding.builder()
                        .user(seller1)
                        .stock(stock)
                        .quantity(10L)
                        .avgBuyPrice(new BigDecimal("90"))
                        .build()
        );

        String firstSellJson = """
            {
                "symbol": "TESTSTOCK",
                "side": "SELL",
                "orderType": "LIMIT",
                "quantity": 10,
                "price": 100
            }
            """;

        mockMvc.perform(
                        post("/api/orders")
                                .header(
                                        "Authorization",
                                        "Bearer " + seller1Token
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(firstSellJson)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"));

        String firstBuyJson = """
            {
                "symbol": "TESTSTOCK",
                "side": "BUY",
                "orderType": "LIMIT",
                "quantity": 10,
                "price": 100
            }
            """;

        mockMvc.perform(
                post("/api/orders")
                        .header(
                                "Authorization",
                                "Bearer " + buyerToken
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firstBuyJson)

        )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("FILLED"));

        Holding holdingAfterBuy =
                holdingRepository.findByUser_IdAndStock_Id(
                        buyerId,
                        stock.getId()
                ).orElseThrow();

        assertThat(holdingAfterBuy.getQuantity())
                .isEqualTo(10L);

        assertThat(holdingAfterBuy.getAvgBuyPrice())
                .isEqualByComparingTo("100");


        User seller2 = userRepository.findById(seller2Id)
                .orElseThrow();

        holdingRepository.save(
                Holding.builder()
                        .user(seller2)
                        .stock(stock)
                        .quantity(4L)
                        .avgBuyPrice(new BigDecimal("100"))
                        .build()
        );

        String secondSellJson = """
                
                {
                    "symbol": "TESTSTOCK",
                    "side": "SELL",
                    "orderType": "LIMIT",
                    "quantity": 4,
                    "price": 110
                
                }
                
                """;

        mockMvc.perform(
                post("/api/orders")
                        .header("Authorization","Bearer "+ buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(secondSellJson)
        )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"));

        walletService.credit(
                seller2Id,
                new BigDecimal("100000")
        );

        String secondBuyJson = """
            {
                "symbol": "TESTSTOCK",
                "side": "BUY",
                "orderType": "LIMIT",
                "quantity": 4,
                "price": 110
            }
            """;

        mockMvc.perform(
                        post("/api/orders")
                                .header(
                                        "Authorization",
                                        "Bearer " + seller2Token
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(secondBuyJson)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("FILLED"));

        Holding finalHolding =
                holdingRepository.findByUser_IdAndStock_Id(
                        buyerId,
                        stock.getId()
                ).orElseThrow();

        assertThat(finalHolding.getQuantity())
                .isEqualTo(6L);

        assertThat(finalHolding.getAvgBuyPrice())
                .isEqualByComparingTo("100");
    }


    @Test
    void sellOrder_fullyLiquidated_deletesHolding() throws Exception {


        RegisterRequest buyerReq = new RegisterRequest(
                "Buyer",
                "buyer" + System.currentTimeMillis() + "@example.com",
                "SecurePass123"
        );

        String buyerResponse = mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(buyerReq))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode buyerJson = objectMapper.readTree(buyerResponse);

        String buyerToken = buyerJson.get("token").asText();
        Long buyerId = buyerJson.get("userId").asLong();


        RegisterRequest sellerReq = new RegisterRequest(
                "Seller",
                "seller" + System.currentTimeMillis() + "@example.com",
                "SecurePass123"
        );

        String sellerResponse = mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(sellerReq))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode sellerJson = objectMapper.readTree(sellerResponse);

        String sellerToken = sellerJson.get("token").asText();
        Long sellerId = sellerJson.get("userId").asLong();


        Stock stock = stockRepository.save(
                Stock.builder()
                        .symbol("TESTSTOCK")
                        .name("Test Stock")
                        .currentPrice(new BigDecimal("100"))
                        .build()
        );


        walletService.credit(
                buyerId,
                new BigDecimal("100000")
        );


        User seller = userRepository.findById(sellerId)
                .orElseThrow();

        holdingRepository.save(
                Holding.builder()
                        .user(seller)
                        .stock(stock)
                        .quantity(10L)
                        .avgBuyPrice(new BigDecimal("90"))
                        .build()
        );


        String sellJson = """
            {
                "symbol": "TESTSTOCK",
                "side": "SELL",
                "orderType": "LIMIT",
                "quantity": 10,
                "price": 100
            }
            """;

        mockMvc.perform(
                        post("/api/orders")
                                .header(
                                        "Authorization",
                                        "Bearer " + sellerToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(sellJson)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"));


        String buyJson = """
            {
                "symbol": "TESTSTOCK",
                "side": "BUY",
                "orderType": "LIMIT",
                "quantity": 10,
                "price": 100
            }
            """;

        mockMvc.perform(
                        post("/api/orders")
                                .header(
                                        "Authorization",
                                        "Bearer " + buyerToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(buyJson)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("FILLED"));


        Optional<Holding> buyerHoldingAfterBuy =
                holdingRepository.findByUser_IdAndStock_Id(
                        buyerId,
                        stock.getId()
                );

        assertThat(buyerHoldingAfterBuy).isPresent();

        assertThat(buyerHoldingAfterBuy.get().getQuantity())
                .isEqualTo(10L);

        assertThat(buyerHoldingAfterBuy.get().getAvgBuyPrice())
                .isEqualByComparingTo("100");


        String buyerSellJson = """
            {
                "symbol": "TESTSTOCK",
                "side": "SELL",
                "orderType": "LIMIT",
                "quantity": 10,
                "price": 100
            }
            """;

        mockMvc.perform(
                        post("/api/orders")
                                .header(
                                        "Authorization",
                                        "Bearer " + buyerToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(buyerSellJson)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"));


        walletService.credit(sellerId, new BigDecimal("100000"));
        String sellerBuyJson = """
            {
                "symbol": "TESTSTOCK",
                "side": "BUY",
                "orderType": "LIMIT",
                "quantity": 10,
                "price": 100
            }
            """;

        mockMvc.perform(
                        post("/api/orders")
                                .header(
                                        "Authorization",
                                        "Bearer " + sellerToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(sellerBuyJson)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("FILLED"));



        Optional<Holding> buyerHoldingAfterSell =
                holdingRepository.findByUser_IdAndStock_Id(
                        buyerId,
                        stock.getId()
                );

        assertThat(buyerHoldingAfterSell)
                .isEmpty();
    }

    @Test
    void sellOrder_withInsufficientHoldings_returnsConflict() throws Exception {

        RegisterRequest buyerReq = new RegisterRequest(
                "Buyer",
                "buyer" + System.currentTimeMillis() + "@example.com",
                "SecurePass123"
        );

        String buyerResponse = mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(buyerReq))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode buyerJson = objectMapper.readTree(buyerResponse);

        String buyerToken = buyerJson.get("token").asText();
        Long buyerId = buyerJson.get("userId").asLong();

        Stock stock = stockRepository.save(
                Stock.builder()
                        .symbol("TESTSTOCK")
                        .name("Test Stock")
                        .currentPrice(new BigDecimal("100"))
                        .build()
        );



        User buyer = userRepository.findById(buyerId)
                .orElseThrow();

        holdingRepository.save(
                Holding.builder()
                        .user(buyer)
                        .stock(stock)
                        .quantity(10L)
                        .avgBuyPrice(new BigDecimal("100"))
                        .build()
        );

        Optional<Holding> initialHolding =
                holdingRepository.findByUser_IdAndStock_Id(
                        buyerId,
                        stock.getId()
                );

        assertThat(initialHolding).isPresent();

        assertThat(initialHolding.get().getQuantity())
                .isEqualTo(10L);


        String sellJson = """
            {
                "symbol": "TESTSTOCK",
                "side": "SELL",
                "orderType": "LIMIT",
                "quantity": 15,
                "price": 100
            }
            """;

        mockMvc.perform(
                        post("/api/orders")
                                .header(
                                        "Authorization",
                                        "Bearer " + buyerToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(sellJson)
                )
                .andExpect(status().isConflict());

        List<Order> orders =
                orderRepository.findByUserId(buyerId);

        assertThat(orders)
                .isEmpty();


        Optional<Holding> holdingAfterRejectedOrder =
                holdingRepository.findByUser_IdAndStock_Id(
                        buyerId,
                        stock.getId()
                );

        assertThat(holdingAfterRejectedOrder)
                .isPresent();

        assertThat(holdingAfterRejectedOrder.get().getQuantity())
                .isEqualTo(10L);

        assertThat(holdingAfterRejectedOrder.get().getAvgBuyPrice())
                .isEqualByComparingTo("100");
    }

    @Test
    void getPortfolio_returnsHoldingsWithCorrectPnL() throws Exception{

        RegisterRequest buyerReq = new RegisterRequest(
                "Buyer",
                "buyer" + System.currentTimeMillis() + "@example.com",
                "SecurePass123"
        );

        String buyerResponse = mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(buyerReq))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode buyerJson = objectMapper.readTree(buyerResponse);

        String buyerToken = buyerJson.get("token").asText();
        Long buyerId = buyerJson.get("userId").asLong();

        RegisterRequest sellerReq = new RegisterRequest(
                "Seller",
                "seller" + System.currentTimeMillis() + "@example.com",
                "SecurePass123"
        );

        String sellerResponse = mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(sellerReq))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode sellerJson = objectMapper.readTree(sellerResponse);

        String sellerToken = sellerJson.get("token").asText();
        Long sellerId = sellerJson.get("userId").asLong();

        Stock stock = stockRepository.save(
                Stock.builder()
                        .symbol("TESTSTOCK")
                        .name("Test Stock")
                        .currentPrice(new BigDecimal("120"))
                        .build()
        );

        walletService.credit(
                buyerId,
                new BigDecimal("100000")
        );

        User seller = userRepository.findById(sellerId)
                .orElseThrow();

        holdingRepository.save(
                Holding.builder()
                        .user(seller)
                        .stock(stock)
                        .quantity(10L)
                        .avgBuyPrice(new BigDecimal("90"))
                        .build()
        );

        String sellJson = """
            {
                "symbol": "TESTSTOCK",
                "side": "SELL",
                "orderType": "LIMIT",
                "quantity": 10,
                "price": 100
            }
            """;

        mockMvc.perform(
                        post("/api/orders")
                                .header(
                                        "Authorization",
                                        "Bearer " + sellerToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(sellJson)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"));


        String buyJson = """
            {
                "symbol": "TESTSTOCK",
                "side": "BUY",
                "orderType": "LIMIT",
                "quantity": 10,
                "price": 100
            }
            """;

        mockMvc.perform(
                        post("/api/orders")
                                .header(
                                        "Authorization",
                                        "Bearer " + buyerToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(buyJson)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("FILLED"));

        mockMvc.perform(
                get("/api/portfolio")
                        .header(
                                "Authorization",
                                "Bearer " + buyerToken
                        )
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].symbol")
                        .value("TESTSTOCK"))
                .andExpect(jsonPath("$[0].quantity")
                        .value(10))
                .andExpect(jsonPath("$[0].avgBuyPrice")
                        .value(100))
                .andExpect(jsonPath("$[0].currentPrice")
                        .value(120))
                .andExpect(jsonPath("$[0].unrealizedPnL")
                        .value(200));
    }

    @Test
    void getHoldingBySymbol_unknownSymbol_returnsNotFound() throws Exception{

        RegisterRequest userReq = new RegisterRequest(
                "Portfolio User",
                "portfolio" + System.currentTimeMillis() + "@example.com",
                "SecurePass123"
        );

        String response = mockMvc.perform(
                post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userReq))
        )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);

        String token = json.get("token").asText();
        Long userId = json.get("userId").asLong();

        mockMvc.perform(
                get("/api/portfolio/UNKNOWNSTOCK")
                        .header(
                                "Authorization",
                                "Bearer " + token
                        )
        )
                .andExpect(status().isNotFound());
    }
}
