package com.screener.stockproducer.model;

public record StockMessage(
        StockUpdate stockUpdate,
        String tracingId
) {
}
