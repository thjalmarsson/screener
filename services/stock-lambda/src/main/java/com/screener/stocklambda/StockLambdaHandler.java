package com.screener.stocklambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.screener.stocklambda.model.Stock;
import com.screener.stocklambda.model.StockScreenRequest;
import com.screener.stocklambda.model.StockScreenResponse;
import com.screener.stocklambda.repository.StockRepository;
import com.screener.stocklambda.service.StockService;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.List;

public class StockLambdaHandler implements RequestHandler<StockScreenRequest, List<Stock>> {

    private final StockService stockService;

    public StockLambdaHandler() {
        DynamoDbClient dynamoDbClient = DynamoDbClient.create();
        StockRepository stockRepository = new StockRepository(dynamoDbClient);
        this.stockService = new StockService(stockRepository);
    }

    @Override
    public List<Stock> handleRequest(StockScreenRequest stockScreenRequest, Context context) {
        context.getLogger().log("Received stock screening request: " + stockScreenRequest);
        return stockService.screen(stockScreenRequest);
    }
}
