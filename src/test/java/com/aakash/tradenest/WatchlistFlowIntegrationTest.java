package com.aakash.tradenest;

import com.aakash.tradenest.stock.entity.Stock;
import com.aakash.tradenest.stock.repository.StockRepository;
import com.aakash.tradenest.user.dto.RegisterRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class WatchlistFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StockRepository stockRepository;

    @Test
    void addToWatchlist_thenAppearsInGetWatchlist() throws Exception{

        RegisterRequest registerReq = new RegisterRequest(
                "Watchlist Test",
                "user" + System.currentTimeMillis() + "@example.com",
                "SecurePass123"
        );

        String registerResponse = mockMvc.perform(
                post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq))
        ).andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode registerJson = objectMapper.readTree(registerResponse);

        String token = registerJson.get("token").asText();

        stockRepository.save(Stock.builder()
                .symbol("TESTSTOCK")
                .name("Test Stock Inc")
                .currentPrice(new BigDecimal("100.0"))
                .build()
        );


        mockMvc.perform(
                post("/api/watchlist/TESTSTOCK")
                        .header("Authorization", "Bearer " + token)
        ).andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("TESTSTOCK"))
                .andExpect(jsonPath("$.name").value("Test Stock Inc"))
                .andExpect(jsonPath("$.currentPrice").value(100.0));

        mockMvc.perform(
                get("/api/watchlist")
                        .header("Authorization", "Bearer " + token)
        ).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].symbol").value("TESTSTOCK"))
                .andExpect(jsonPath("$[0].name").value("Test Stock Inc"))
                .andExpect(jsonPath("$[0].currentPrice").value(100.0));
    }

    @Test
    void addToWatchlist_calledTwice_isIdempotent() throws Exception{

        RegisterRequest registerReq = new RegisterRequest(
                "Watchlist Test",
                "user" + System.currentTimeMillis() + "@example.com",
                "SecurePass123"
        );

        String registerResponse = mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(registerReq))
                ).andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode registerJson = objectMapper.readTree(registerResponse);

        String token = registerJson.get("token").asText();

        stockRepository.save(Stock.builder()
                .symbol("TESTSTOCK")
                .name("Test Stock Inc")
                .currentPrice(new BigDecimal("100.0"))
                .build()
        );


        mockMvc.perform(
                        post("/api/watchlist/TESTSTOCK")
                                .header("Authorization", "Bearer " + token)
                ).andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("TESTSTOCK"))
                .andExpect(jsonPath("$.name").value("Test Stock Inc"))
                .andExpect(jsonPath("$.currentPrice").value(100.0));

        mockMvc.perform(
                        post("/api/watchlist/TESTSTOCK")
                                .header("Authorization", "Bearer " + token)
                ).andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("TESTSTOCK"))
                .andExpect(jsonPath("$.name").value("Test Stock Inc"))
                .andExpect(jsonPath("$.currentPrice").value(100.0));

        mockMvc.perform(
                        get("/api/watchlist")
                                .header("Authorization", "Bearer " + token)
                ).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].symbol").value("TESTSTOCK"))
                .andExpect(jsonPath("$[0].name").value("Test Stock Inc"))
                .andExpect(jsonPath("$[0].currentPrice").value(100.0))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void removeFromWatchlist_removesIt() throws Exception{
        RegisterRequest registerRequest = new RegisterRequest(
                "Remove Test",
                "remove" + System.currentTimeMillis() + "@example.com",
                "SecurePass123"
        );

        String registerResponse = mockMvc.perform(
                post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest))

        ).andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode registerJson = objectMapper.readTree(registerResponse);

        String token = registerJson.get("token").asText();

        stockRepository.save(Stock.builder()
                .symbol("TESTSTOCK")
                .name("TestStock Inc")
                .currentPrice(new BigDecimal("100.0")
                        )
                .build()
        );

        mockMvc.perform(
                post("/api/watchlist/TESTSTOCK")
                        .header("Authorization","Bearer "+ token)
        ).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("TestStock Inc"))
                .andExpect(jsonPath("$.symbol").value("TESTSTOCK"))
                .andExpect(jsonPath("$.currentPrice").value(100.0));

        mockMvc.perform(
                delete("/api/watchlist/TESTSTOCK")
                        .header("Authorization", "Bearer " + token)
        );

        mockMvc.perform(
                        get("/api/watchlist")
                                .header("Authorization", "Bearer " + token)
                ).andExpect(status().isOk())

                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void removeFromWatchlist_neverAdded_returnsNotFound() throws Exception{
        RegisterRequest registerRequest = new RegisterRequest(
                "Remove Test",
                "remove" + System.currentTimeMillis() + "@example.com",
                "SecurePass123"
        );

        String registerResponse = mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(registerRequest))

                ).andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode registerJson = objectMapper.readTree(registerResponse);

        String token = registerJson.get("token").asText();

        stockRepository.save(Stock.builder()
                .symbol("TESTSTOCK")
                .name("TestStock Inc")
                .currentPrice(new BigDecimal("100.0")
                )
                .build()
        );



        mockMvc.perform(
                        delete("/api/watchlist/TESTSTOCK")
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isNotFound());


    }

    @Test
    void addToWatchlist_unknownSymbol_returnsNotFound() throws Exception {

        RegisterRequest registerRequest = new RegisterRequest(
                "Unknown Stock Test",
                "unknown" + System.currentTimeMillis() + "@example.com",
                "SecurePass123"
        );

        String registerResponse = mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(registerRequest))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode registerJson = objectMapper.readTree(registerResponse);

        String token = registerJson.get("token").asText();

        mockMvc.perform(
                        post("/api/watchlist/UNKNOWNSTOCK")
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void getWatchlist_includesLiveCurrentPrice() throws Exception {

        RegisterRequest registerRequest = new RegisterRequest(
                "Live Price Test",
                "liveprice" + System.currentTimeMillis() + "@example.com",
                "SecurePass123"
        );

        String registerResponse = mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(registerRequest))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode registerJson = objectMapper.readTree(registerResponse);

        String token = registerJson.get("token").asText();

        Stock testStock = stockRepository.save(
                Stock.builder()
                        .symbol("TESTSTOCK")
                        .name("Test Stock Inc")
                        .currentPrice(new BigDecimal("100.00"))
                        .build()
        );

        mockMvc.perform(
                        post("/api/watchlist/TESTSTOCK")
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isOk());

        testStock.setCurrentPrice(new BigDecimal("125.50"));
        stockRepository.save(testStock);

        mockMvc.perform(
                        get("/api/watchlist")
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].symbol").value("TESTSTOCK"))
                .andExpect(jsonPath("$[0].name").value("Test Stock Inc"))
                .andExpect(jsonPath("$[0].currentPrice").value(125.50));
    }

}
