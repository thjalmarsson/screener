package com.screener.stock_service.service;

import com.screener.stock_service.model.SortField;
import com.screener.stock_service.model.SortOrder;
import com.screener.stock_service.model.Stock;
import com.screener.stock_service.model.StockScreenRequest;
import com.screener.stock_service.repository.StockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class StockService {

    private final static Logger log = LoggerFactory.getLogger(StockService.class);

    private final StockRepository stockRepository;

    public StockService(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    public Optional<Stock> findBySymbol(String symbol) {
        return stockRepository.findBySymbol(symbol);
    }

    // TODO: Just for testing purposes, to be removed
    public List<Stock> findByLowestPeRatios(int limit) {
        return stockRepository.findLowestPeRatios(limit);
    }

    public List<Stock> screen(StockScreenRequest stockScreenRequest) {
        if (stockScreenRequest.sortBy() == SortField.PE_RATIO) {
            boolean ascending = stockScreenRequest.sortOrder() == SortOrder.ASC;
            return stockRepository.findByRatios(stockScreenRequest.limit(), ascending);
        }

        throw new IllegalArgumentException("Unsported field from request with request: " + stockScreenRequest);
    }

}
