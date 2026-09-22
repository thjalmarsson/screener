package com.screener.queryservice.interpreter;

import com.screener.queryservice.client.SortField;
import com.screener.queryservice.client.SortOrder;

public record QueryInterpretation(
        boolean supported,
        String reason,
        SortField sortBy,
        SortOrder sortOrder,
        Integer limit
) {
}
