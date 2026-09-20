package com.screener.stock_service.service;

import com.screener.stock_service.model.Stock;
import com.screener.stock_service.repository.StockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StockServiceTest {

    private StockRepository stockRepository;
    private StockService stockService;

    @BeforeEach
    void setUp() {
        stockRepository = mock(StockRepository.class);
        stockService = new StockService(stockRepository);
    }

    @Test
    void shouldReturnStockWhenStockExists() {

        Stock stock = new Stock(
                "AAPL",
                "Apple Inc.",
                new BigDecimal("255.00"),
                new BigDecimal("34.5"),
                new BigDecimal("3800000000000"),
                "Technology"
        );

        when(stockRepository.findBySymbol("AAPL"))
                .thenReturn(Optional.of(stock));

        Optional<Stock> result = stockService.findBySymbol("AAPL");

        assertThat(result).contains(stock);

        verify(stockRepository).findBySymbol("AAPL");
    }

    @Test
    void shouldReturnEmptyWhenStockDoesNotExist() {

        when(stockRepository.findBySymbol("UNKNOWN"))
                .thenReturn(Optional.empty());

        Optional<Stock> result = stockService.findBySymbol("UNKNOWN");

        assertThat(result).isEmpty();

        verify(stockRepository).findBySymbol("UNKNOWN");
    }
}