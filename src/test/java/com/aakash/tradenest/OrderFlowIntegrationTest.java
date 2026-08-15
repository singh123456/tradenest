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
import java.math.RoundingMode;
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
public class OrderFlowIntegrationTest {

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
    private  OrderMatchingEngine orderMatchingEngine;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TradeRepository tradeRepository;

    @Autowired
    private HoldingRepository holdingRepository;

    @Test
    void placeOrder_withSufficientBalance_returnsCreated() throws Exception{

        RegisterRequest register = new RegisterRequest("Sufficient Balance", "sufficient@example.com", "SecurePass123");

        String registerResponse = mockMvc.perform(post("/api/auth/register")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(register)))
                        .andExpect(status().isCreated())
                                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(registerResponse).get("token").asText();
        Long userId = objectMapper.readTree(registerResponse).get("userId").asLong();


        walletService.credit(userId,new BigDecimal("100000"));

        stockRepository.save(Stock.builder()
                .symbol("TESTSTOCK")
                .name("Test Stock Inc")
                        .currentPrice(new BigDecimal("100.00"))
                .build());

        String orderRequestJson = """
                {
                "symbol":"TESTSTOCK",
                "side": "BUY",
                "orderType": "LIMIT",
                "quantity": 10,
                "price": 100.00
                }
                """;


        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer "+token)
                        .contentType("application/json")
                        .content(orderRequestJson))


                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.symbol").value("TESTSTOCK"))
                .andExpect(jsonPath("$.side").value("BUY"))
                .andExpect(jsonPath("$.quantity").value(10))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void placeOrder_withInSufficientBalance_returnsConflict() throws Exception{

        RegisterRequest register = new RegisterRequest("InSufficient Balance", "insufficient@example.com", "SecurePass123");

        String registerResponse = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(registerResponse).get("token").asText();
        Long userId = objectMapper.readTree(registerResponse).get("userId").asLong();


        walletService.credit(userId,new BigDecimal("10"));

        stockRepository.save(Stock.builder()
                .symbol("TESTSTOCK")
                .name("Test Stock Inc")
                .currentPrice(new BigDecimal("100.00"))
                .build());

        String orderRequestJson = """
                {
                "symbol":"TESTSTOCK",
                "side": "BUY",
                "orderType": "LIMIT",
                "quantity": 10,
                "price": 100.00
                }
                """;


        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer "+token)
                        .contentType("application/json")
                        .content(orderRequestJson))


                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Insufficient balance to place this order"));

    }

    @Test
    void placeOrder_withMarketType_returnsBadRequest() throws Exception{

        RegisterRequest register = new RegisterRequest("Market-Type", "market-type@example.com", "SecurePass123");

        String registerResponse = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(registerResponse).get("token").asText();
        Long userId = objectMapper.readTree(registerResponse).get("userId").asLong();


        walletService.credit(userId,new BigDecimal("100000"));

        stockRepository.save(Stock.builder()
                .symbol("TESTSTOCK")
                .name("Test Stock Inc")
                .currentPrice(new BigDecimal("100.00"))
                .build());

        String orderRequestJson = """
                {
                "symbol":"TESTSTOCK",
                "side": "BUY",
                "orderType": "MARKET",
                "quantity": 10,
                "price": 100.00
                }
                """;


        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer "+token)
                        .contentType("application/json")
                        .content(orderRequestJson))


                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Only LIMIT orders are supported at this time"));
    }

    @Test
    void placeOrder_withUnknownSymbol_returnsNotFound() throws Exception{

        RegisterRequest register = new RegisterRequest("Unknown Symbol", "unknown-symbol@example.com", "SecurePass123");

        String registerResponse = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(registerResponse).get("token").asText();
        Long userId = objectMapper.readTree(registerResponse).get("userId").asLong();


        walletService.credit(userId,new BigDecimal("100000"));

        stockRepository.save(Stock.builder()
                .symbol("TESTSTOCK")
                .name("Test Stock Inc")
                .currentPrice(new BigDecimal("100.00"))
                .build());

        String orderRequestJson = """
                {
                "symbol":"TESTSTOCKSFORYOU",
                "side": "BUY",
                "orderType": "LIMIT",
                "quantity": 10,
                "price": 100.00
                }
                """;


        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer "+token)
                        .contentType("application/json")
                        .content(orderRequestJson))


                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No stock found with symbol: TESTSTOCKSFORYOU"));
    }

    @Test
    void twoMatchingOrders_actuallyExecuteATrade() throws Exception{

        RegisterRequest buyerReq = new RegisterRequest("Buyer", "buyer"+ System.currentTimeMillis()+ "@example.com", "SecurePass123");

        String buyerResponse = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(buyerReq)))
                .andReturn().getResponse().getContentAsString();

        String buyerToken = objectMapper.readTree(buyerResponse).get("token").asText();
        Long buyerId = objectMapper.readTree(buyerResponse).get("userId").asLong();

        RegisterRequest sellerReq = new RegisterRequest("Seller", "seller"+ System.currentTimeMillis()+ "@example.com", "SecurePass123");

        String sellerResponse = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(sellerReq)))
                .andReturn().getResponse().getContentAsString();

        String sellerToken = objectMapper.readTree(sellerResponse).get("token").asText();
        Long sellerId = objectMapper.readTree(sellerResponse).get("userId").asLong();

        walletService.credit(buyerId,new BigDecimal("100000"));

        walletService.credit(sellerId,new BigDecimal("1000"));



      Stock stock =  stockRepository.save(Stock.builder()
                .symbol("TESTSTOCK")
                .name("Test Stock Inc")
                .currentPrice(new BigDecimal("100"))
                .build());

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
                "symbol":"TESTSTOCK",
                "side": "SELL",
                "orderType": "LIMIT",
                "quantity": 10,
                "price": 100
                }
                """;


        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer "+sellerToken)
                        .contentType("application/json")
                        .content(sellJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"));

        String buyJson = """
                {
                "symbol":"TESTSTOCK",
                "side": "BUY",
                "orderType": "LIMIT",
                "quantity": 10,
                "price": 100
                }
                """;


        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer "+buyerToken)
                        .contentType("application/json")
                        .content(buyJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("FILLED"));


        List<Order> orders = orderRepository.findAll();
        assertThat(orders).hasSize(2);

        for(Order order:orders){
            assertThat(order.getStatus()).isEqualTo(OrderStatus.FILLED);
            assertThat(order.getFilledQuantity()).isEqualTo(order.getQuantity());

        }

        List<Trade> trades = tradeRepository.findAll();
        assertThat(trades).hasSize(1);

        Trade trade = trades.get(0);

        assertThat(trade.getQuantity()).isEqualTo(10L);
        assertThat(trade.getPrice()).isEqualByComparingTo("100");
    }

    @Test
    void getMyOrders_returnsOnlyCallersOrders() throws Exception{

        RegisterRequest register = new RegisterRequest("My Orders", "my-orders@example.com", "SecurePass123");

        String registerResponse = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(registerResponse).get("token").asText();
        Long userId = objectMapper.readTree(registerResponse).get("userId").asLong();


        walletService.credit(userId,new BigDecimal("100000"));

        stockRepository.save(Stock.builder()
                .symbol("TESTSTOCK")
                .name("Test Stock Inc")
                .currentPrice(new BigDecimal("100.00"))
                .build());



        String orderRequestJson = """
        {
        "symbol":"TESTSTOCK",
        "side": "BUY",
        "orderType": "LIMIT",
        "quantity": 10,
        "price": 100.00
        }
        """;

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(orderRequestJson))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].symbol").value("TESTSTOCK"));
    }




}
