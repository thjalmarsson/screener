package com.screener.stockingestion.model;

public record StockMessage(
        StockUpdate stockUpdate,
        String tracingId
) {
}
