package com.screener.stocklambda.model;

public record StockScreenRequest(
        SortField sortBy,
        SortOrder sortOrder,
        int limit
) {
}
