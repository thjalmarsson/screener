package com.screener.stock_service.model;

public record StockScreenRequest(
        SortField sortBy,
        SortOrder sortOrder,
        int limit
) {
}
