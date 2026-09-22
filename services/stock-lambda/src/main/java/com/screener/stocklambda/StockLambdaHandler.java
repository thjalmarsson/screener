package com.screener.stocklambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.screener.stocklambda.model.Stock;
import com.screener.stocklambda.model.StockScreenRequest;
import com.screener.stocklambda.repository.StockRepository;
import com.screener.stocklambda.service.StockService;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.List;
import java.util.Map;

public class StockLambdaHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final StockService stockService;
    private final ObjectMapper objectMapper;
    private final Map<String, String> headers = Map.of("Content-Type", "application/json");

    public StockLambdaHandler() {
        DynamoDbClient dynamoDbClient = DynamoDbClient.create();
        StockRepository stockRepository = new StockRepository(dynamoDbClient);
        this.stockService = new StockService(stockRepository);
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        try {
            context.getLogger().log(
                    "Received request: " + event.getBody()
            );

            StockScreenRequest stockScreenRequest = objectMapper
                    .readValue(
                            event.getBody(),
                            StockScreenRequest.class
                    );
            List<Stock> stockList = stockService.screen(stockScreenRequest);
            String responseBody = objectMapper.writeValueAsString(stockList);

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withHeaders(headers)
                    .withBody(responseBody);
        } catch (JsonProcessingException e) {
            context.getLogger().log("Invalid request: " + e.getMessage());
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withBody("{\"error\":\"Invalid request\"}");
        } catch (Exception e) {
            context.getLogger().log(
                    "Unexpected error: " + e
            );
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody(
                            "{\"error\":\"Internal server error\"}"
                    );
        }
    }
}
