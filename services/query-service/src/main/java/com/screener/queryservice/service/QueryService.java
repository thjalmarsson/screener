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
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class QueryService {

    private static final Logger log = LoggerFactory.getLogger(QueryService.class);
    private final StockServiceClient stockServiceClient;
    private final QueryInterpreter queryInterpreter;
    private final StockScreenRequestValidator stockScreenRequestValidator;

    private final static int DEFAULT_LIMIT = 5;
    private final static int QUERY_MAX_LENGTH = 100;

    public QueryService(StockServiceClient stockServiceClient, QueryInterpreter queryInterpreter, StockScreenRequestValidator stockScreenRequestValidator) {
        this.stockServiceClient = stockServiceClient;
        this.queryInterpreter = queryInterpreter;
        this.stockScreenRequestValidator = stockScreenRequestValidator;
    }

    public List<StockResponse> query(String query) {
        validateQuery(query);
        QueryInterpretation queryInterpretation = queryInterpreter.interpret(query);

        if (!queryInterpretation.supported()) {
            log.debug("Invalid query interpreted for query: {} \n Resulted in intepretation:{}", query, queryInterpretation);
            throw new UnsupportedQueryException(queryInterpretation.reason());
        }
        Integer interpretedLimit = queryInterpretation.limit();

        if (interpretedLimit == null) {
            log.debug(
                    "Model set limit to null, defaulting to DEFAULT_LIMIT={}",
                    DEFAULT_LIMIT
            );
        }

        int limit = interpretedLimit != null
                ? interpretedLimit
                : DEFAULT_LIMIT;

        StockScreenRequest stockScreenRequest = new StockScreenRequest(
                queryInterpretation.sortBy(),
                queryInterpretation.sortOrder(),
                limit
        );


        stockScreenRequestValidator.validate(stockScreenRequest);
        return stockServiceClient.screen(stockScreenRequest);
    }

    private void validateQuery(String query) {
        if (query.isBlank()) {
            throw new UnsupportedQueryException("Query is blank or empty");
        }

        if (query == null) {
            throw new UnsupportedQueryException("Query is null");
        }

        if (query.length() > QUERY_MAX_LENGTH) {
            throw new UnsupportedQueryException("Query is to long, max allowed length is " + QUERY_MAX_LENGTH + ", but received query with length: " + query.length());
        }
    }
}