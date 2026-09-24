package com.screener.stockproducer.model;

import java.math.BigDecimal;

public record StockUpdate(
        String symbol,
        String name,
        BigDecimal price,
        BigDecimal peRatio,
        BigDecimal marketCap,
        String sector
) {
}

