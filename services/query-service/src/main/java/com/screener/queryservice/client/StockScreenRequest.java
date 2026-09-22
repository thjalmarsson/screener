package com.screener.queryservice.client;

public record StockScreenRequest(
        SortField sortBy,
        SortOrder sortOrder,
        int limit
) {
}