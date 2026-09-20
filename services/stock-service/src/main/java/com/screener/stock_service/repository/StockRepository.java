package com.screener.stock_service.repository;

import com.screener.stock_service.model.SortField;
import com.screener.stock_service.model.SortOrder;
import com.screener.stock_service.model.Stock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import javax.swing.text.html.Option;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class StockRepository {

    private final static Logger log = LoggerFactory.getLogger(StockRepository.class);

    private final DynamoDbClient dynamoDbClient;

    public StockRepository(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    public Optional<Stock> findBySymbol(String symbol) {
        log.debug("Trying to find Stock with symbol: {}", symbol);
        Map<String, AttributeValue> key = Map.of(
                "symbol",
                AttributeValue.builder()
                        .s(symbol)
                        .build()
        );

        GetItemRequest request = GetItemRequest.builder()
                .tableName("Stocks")
                .key(key)
                .build();

        GetItemResponse response = dynamoDbClient.getItem(request);

        if (!response.hasItem()) {
            log.debug("Could not find Stock with symbol: {}", symbol);
            return Optional.empty();
        }

        Map<String, AttributeValue> item = response.item();

        Stock stock = toStock(item);

        log.debug("Returning Stock with symbol: {}", symbol);
        return Optional.of(stock);
    }

    public List<Stock> findLowestPeRatios(int limit) {

        log.debug("Finding {} stocks with lowest P/E ratios", limit);

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
                .scanIndexForward(true)
                .limit(limit)
                .build();

        QueryResponse response = dynamoDbClient.query(request);

        return response.items()
                .stream()
                .map(this::toStock)
                .toList();
    }

    public List<Stock> findByRatios(int limit, boolean ascending) {

        log.debug("Finding {} stocks by P/E ratios, in order ascending={}", limit, ascending);

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

    public List<Stock> findHighestPeRatios(int limit) {

        log.debug("Finding {} stocks with highest P/E ratios", limit);

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
                .scanIndexForward(false)
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