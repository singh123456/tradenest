package com.aakash.tradenest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class StockFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getAllStocks_noTokenRequired_returnsSeededStocks() throws Exception{
        mockMvc.perform(get("/api/stocks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(10))
                .andExpect(jsonPath("$[*].symbol",org.hamcrest.Matchers.hasItem("RELIANCE")));
    }

    @Test
    void getStockBySymbol_existingSymbol_returnsStock() throws Exception{
        mockMvc.perform(get("/api/stocks/RELIANCE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("RELIANCE"))
                .andExpect(jsonPath("$.name").value("Reliance Industries Ltd"))
                .andExpect(jsonPath("$.currentPrice").isNotEmpty());
    }

    @Test
    void getStockBySymbol_unknowSymbol_returnsNotFound() throws Exception{
        mockMvc.perform(get("/api/stocks/FAKESYMBOL"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("No stock found with symbol: FAKESYMBOL"));
    }
}
