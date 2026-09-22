package com.screener.queryservice.service;

import com.screener.queryservice.client.*;
import com.screener.queryservice.exceptions.UnsupportedQueryException;
import com.screener.queryservice.interpreter.QueryInterpretation;
import com.screener.queryservice.interpreter.QueryInterpreter;
import com.screener.queryservice.interpreter.StockScreenRequestValidator;
import com.screener.queryservice.interpreter.ollama.OllamaQueryInterpreter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QueryService {

    private static final Logger log = LoggerFactory.getLogger(QueryService.class);
    private final StockServiceClient stockServiceClient;
    private final QueryInterpreter queryInterpreter;
    private final StockScreenRequestValidator stockScreenRequestValidator;

    public QueryService(StockServiceClient stockServiceClient, QueryInterpreter queryInterpreter, StockScreenRequestValidator stockScreenRequestValidator) {
        this.stockServiceClient = stockServiceClient;
        this.queryInterpreter = queryInterpreter;
        this.stockScreenRequestValidator = stockScreenRequestValidator;
    }

    public List<StockResponse> query(String query) {
        log.debug("inside queryStockService for query:{}", query);
        QueryInterpretation queryInterpretation = queryInterpreter.interpret(query);

        if (!queryInterpretation.supported()) {
            log.debug("Invalid query interpreted for query: {} \n Resulted in intepretation:{}", query, queryInterpretation);
            throw new UnsupportedQueryException(queryInterpretation.reason());
        }

        StockScreenRequest stockScreenRequest = new StockScreenRequest(
                queryInterpretation.sortBy(),
                queryInterpretation.sortOrder(),
                queryInterpretation.limit()
        );
        stockScreenRequestValidator.validate(stockScreenRequest);
        return stockServiceClient.screen(stockScreenRequest);
    }
}