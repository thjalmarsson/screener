package com.screener.queryservice.client;

import java.math.BigDecimal;

public record StockResponse(
        String symbol,
        String name,
        BigDecimal price,
        BigDecimal peRatio,
        BigDecimal marketCap,
        String sector
) {
}
