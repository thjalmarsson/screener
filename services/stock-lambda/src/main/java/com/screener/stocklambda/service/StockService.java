package com.screener.stocklambda.service;


import com.screener.stocklambda.model.SortField;
import com.screener.stocklambda.model.SortOrder;
import com.screener.stocklambda.model.Stock;
import com.screener.stocklambda.model.StockScreenRequest;
import com.screener.stocklambda.repository.StockRepository;

import java.util.List;

public class StockService {

    private final StockRepository stockRepository;

    public StockService(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    public List<Stock> screen(StockScreenRequest request) {
        if (request.sortBy() == SortField.PE_RATIO) {
            boolean ascending = request.sortOrder() == SortOrder.ASC;

            return stockRepository.findByPERatios(
                    request.limit(),
                    ascending
            );
        }

        throw new IllegalArgumentException(
                "Unsupported sort field: " + request.sortBy()
        );
    }
}