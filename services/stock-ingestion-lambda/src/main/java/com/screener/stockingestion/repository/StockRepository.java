package com.screener.stockingestion.repository;

import com.screener.stockingestion.model.StockUpdate;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;

public class StockRepository {

    private final DynamoDbClient dynamoDbClient;

    public StockRepository(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    public void save(StockUpdate stock) {

        Map<String, AttributeValue> item = Map.of(
                "symbol", string(stock.symbol()),
                "name", string(stock.name()),
                "price", number(stock.price()),
                "peRatio", number(stock.peRatio()),
                "marketCap", number(stock.marketCap()),
                "sector", string(stock.sector()),
                "screeningGroup", string("ALL")
        );

        dynamoDbClient.putItem(request -> request
                .tableName("Stocks")
                .item(item)
        );
    }

    private AttributeValue string(String value) {
        return AttributeValue.builder()
                .s(value)
                .build();
    }

    private AttributeValue number(Number value) {
        return AttributeValue.builder()
                .n(value.toString())
                .build();
    }
}
