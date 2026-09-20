package com.screener.stock_service.controller;

import com.screener.stock_service.model.Stock;
import com.screener.stock_service.service.StockService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StockController.class)
class StockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StockService stockService;

    @Test
    void shouldReturnStockWhenStockExists() throws Exception {

        Stock stock = new Stock(
                "AAPL",
                "Apple Inc.",
                new BigDecimal("255.00"),
                new BigDecimal("34.5"),
                new BigDecimal("3800000000000"),
                "Technology"
        );

        when(stockService.findBySymbol("AAPL"))
                .thenReturn(Optional.of(stock));

        mockMvc.perform(get("/stocks/AAPL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("AAPL"))
                .andExpect(jsonPath("$.name").value("Apple Inc."))
                .andExpect(jsonPath("$.peRatio").value(34.5))
                .andExpect(jsonPath("$.sector").value("Technology"));
    }

    @Test
    void shouldReturn404WhenStockDoesNotExist() throws Exception {

        when(stockService.findBySymbol("UNKNOWN"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/stocks/UNKNOWN"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnOrderedListForPE() throws Exception {
        Stock apple = new Stock(
                "AAPL",
                "Apple Inc.",
                new BigDecimal("255.00"),
                new BigDecimal("22.5"),
                new BigDecimal("3800000000000"),
                "Technology"
        );

        Stock google = new Stock(
                "GOOGLE",
                "Apple Inc.",
                new BigDecimal("255.00"),
                new BigDecimal("34.5"),
                new BigDecimal("3800000000000"),
                "Technology"
        );

        Stock meta = new Stock(
                "META",
                "Apple Inc.",
                new BigDecimal("255.00"),
                new BigDecimal("44.5"),
                new BigDecimal("3800000000000"),
                "Technology"
        );

        List<Stock> stockList = List.of(apple, google, meta);

        when(stockService.findByLowestPeRatios(3)).thenReturn(stockList);
        mockMvc.perform(get("/stocks/lowest-pe?limit=3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].symbol").value("AAPL"))
                .andExpect(jsonPath("$[0].peRatio").value(22.5))
                .andExpect(jsonPath("$[1].symbol").value("GOOGLE"))
                .andExpect(jsonPath("$[1].peRatio").value(34.5))
                .andExpect(jsonPath("$[2].symbol").value("META"))
                .andExpect(jsonPath("$[2].peRatio").value(44.5));
    }

    @Test
    void shouldPassLimitToService() throws Exception {
        when(stockService.findByLowestPeRatios(7))
                .thenReturn(List.of());

        mockMvc.perform(get("/stocks/lowest-pe?limit=7"))
                .andExpect(status().isOk());

        verify(stockService).findByLowestPeRatios(7);
    }

    @Test
    void shouldUseDefaultLimitOfFive() throws Exception {
        when(stockService.findByLowestPeRatios(5))
                .thenReturn(List.of());

        mockMvc.perform(get("/stocks/lowest-pe"))
                .andExpect(status().isOk());

        verify(stockService).findByLowestPeRatios(5);
    }
}