package com.screener.queryservice.interpreter;

import com.screener.queryservice.client.StockScreenRequest;
import org.springframework.stereotype.Component;

@Component
public class StockScreenRequestValidator {

    private static final int MAX_RESPONSE_LIMIT = 100;

    public void validate(StockScreenRequest stockScreenRequest) {
        if (stockScreenRequest == null) {
            throw new IllegalArgumentException(
                    "Stock screening request cannot be null"
            );
        }

        if (stockScreenRequest.sortBy() == null) {
            throw new IllegalArgumentException(
                    "sortBy is required"
            );
        }

        if (stockScreenRequest.sortOrder() == null) {
            throw new IllegalArgumentException(
                    "sortOrder is required"
            );
        }

        if (stockScreenRequest.limit() < 1 || stockScreenRequest.limit() > MAX_RESPONSE_LIMIT) {
            throw new IllegalArgumentException(
                    "limit must be between 1 and " + MAX_RESPONSE_LIMIT
            );
        }
    }
}
