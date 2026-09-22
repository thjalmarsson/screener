package com.screener.stockservice.model;

public record StockScreenRequest(
        SortField sortBy,
        SortOrder sortOrder,
        int limit
) {
}
