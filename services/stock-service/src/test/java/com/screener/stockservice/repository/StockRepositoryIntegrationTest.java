package com.screener.stockservice.repository;

import com.screener.stockservice.model.Stock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class StockRepositoryIntegrationTest {

    @Autowired
    private StockRepository stockRepository;

    @Test
    void shouldFindStockBySymbol() {
        Optional<Stock> result = stockRepository.findBySymbol("AAPL");

        assertThat(result).isPresent();

        Stock stock = result.get();

        assertThat(stock.symbol()).isEqualTo("AAPL");
        assertThat(stock.name()).isEqualTo("Apple Inc.");
        assertThat(stock.peRatio()).isEqualByComparingTo(new BigDecimal("34.5"));
        assertThat(stock.sector()).isEqualTo("Technology");
    }
}