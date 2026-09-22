package com.screener.queryservice.client;

public record StockScreenRequest(
        SortField sortBy,
        SortOrder order,
        int limit
) {
}