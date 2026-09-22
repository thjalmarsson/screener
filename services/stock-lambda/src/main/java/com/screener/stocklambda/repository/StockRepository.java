package com.screener.stocklambda.repository;

import com.screener.stocklambda.model.Stock;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class StockRepository {

    private final DynamoDbClient dynamoDbClient;

    public StockRepository(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    public List<Stock> findByPERatios(int limit, boolean ascending) {

        Map<String, AttributeValue> expressionValues = Map.of(
                ":group",
                AttributeValue.builder()
                        .s("ALL")
                        .build()
        );

        QueryRequest request = QueryRequest.builder()
                .tableName("Stocks")
                .indexName("PeRatioIndex")
                .keyConditionExpression("screeningGroup = :group")
                .expressionAttributeValues(expressionValues)
                .scanIndexForward(ascending)
                .limit(limit)
                .build();

        QueryResponse response = dynamoDbClient.query(request);

        return response.items()
                .stream()
                .map(this::toStock)
                .toList();
    }

    private Stock toStock(Map<String, AttributeValue> item) {
        return new Stock(
                item.get("symbol").s(),
                item.get("name").s(),
                new BigDecimal(item.get("price").n()),
                new BigDecimal(item.get("peRatio").n()),
                new BigDecimal(item.get("marketCap").n()),
                item.get("sector").s()
        );
    }
}
