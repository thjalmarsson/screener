package com.screener.queryservice.controller;

import com.screener.queryservice.client.StockResponse;
import com.screener.queryservice.model.QueryRequest;
import com.screener.queryservice.service.QueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/query")
public class QueryController {

    private static final Logger log = LoggerFactory.getLogger(QueryController.class);

    private final QueryService queryService;

    public QueryController(QueryService queryService) {
        this.queryService = queryService;
    }

    @PostMapping
    public ResponseEntity<List<StockResponse>> query(@RequestBody QueryRequest queryRequest) {
        log.debug("Received query:{}", queryRequest.query());
        List<StockResponse> stockResponsesList = queryService.query(queryRequest.query());
        return ResponseEntity.ok().body(stockResponsesList);
    }
}
