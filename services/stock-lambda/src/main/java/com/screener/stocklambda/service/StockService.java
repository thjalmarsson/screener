package com.screener.stocklambda.service;


import com.screener.stocklambda.model.SortField;
import com.screener.stocklambda.model.SortOrder;
import com.screener.stocklambda.model.Stock;
import com.screener.stocklambda.model.StockScreenRequest;
import com.screener.stocklambda.repository.StockRepository;

import java.util.List;

public class StockService {

    private final StockRepository stockRepository;
    private static final String PE_RATIO_INDEX = "PeRatioIndex";
    private static final String MARKET_CAP_INDEX = "MarketCapIndex";

    public StockService(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    public List<Stock> screen(StockScreenRequest request) {
        boolean ascending = request.sortOrder() == SortOrder.ASC;
        if (request.sortBy() == SortField.PE_RATIO) {
            return stockRepository.findStocks(
                    request.limit(),
                    ascending,
                    PE_RATIO_INDEX
            );
        }

        if (request.sortBy() == SortField.MARKET_CAP) {
            return stockRepository.findStocks(
                    request.limit(),
                    ascending,
                    MARKET_CAP_INDEX
            );
        }

        throw new IllegalArgumentException(
                "Unsupported sort field: " + request.sortBy()
        );
    }
}