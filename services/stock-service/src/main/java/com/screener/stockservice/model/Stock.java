package com.screener.stockservice.model;

import java.math.BigDecimal;

public record Stock(
        String symbol,
        String name,
        BigDecimal price,
        BigDecimal peRatio,
        BigDecimal marketCap,
        String sector
) {
}
