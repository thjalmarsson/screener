package com.screener.queryservice.service;

import com.screener.queryservice.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QueryService {

    private static final Logger log = LoggerFactory.getLogger(QueryService.class);
    private final StockServiceClient stockServiceClient;

    public QueryService(StockServiceClient stockServiceClient) {
        this.stockServiceClient = stockServiceClient;
    }

    public List<StockResponse> query(String query) {
        log.debug("inside queryStockService for query:{}", query);
        StockScreenRequest stockScreenRequest = new StockScreenRequest(
                SortField.PE_RATIO,
                SortOrder.ASC,
                5
        );

        return stockServiceClient.screen(stockScreenRequest);
    }
}