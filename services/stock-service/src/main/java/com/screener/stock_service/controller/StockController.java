package com.screener.stock_service.controller;

import com.screener.stock_service.model.Stock;
import com.screener.stock_service.model.StockScreenRequest;
import com.screener.stock_service.service.StockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/stocks")
public class StockController {

    private static final Logger log = LoggerFactory.getLogger(StockController.class);

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping("/{symbol}")
    public ResponseEntity<Stock> getStock(
            @PathVariable String symbol
    ) {
        log.info("Received GET request for symbol: {}", symbol);
        return stockService.findBySymbol(symbol).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // TODO: Just for testing purposes, to be removed
    @GetMapping("/lowest-pe")
    public List<Stock> getByPeRatios(
            @RequestParam(defaultValue = "5") int limit
    ) {
        log.info("Received request for lowerst P/E stocks with limit: {}", limit);
        return stockService.findByLowestPeRatios(limit);
    }

    @PostMapping("/screen")
    public List<Stock> screen(
            @RequestBody StockScreenRequest stockScreenRequest
            ) {
        return stockService.screen(stockScreenRequest);
    }
}
