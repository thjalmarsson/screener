package com.screener.stockproducer;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.screener.stockproducer.model.StockUpdate;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class StockProducerHandler implements RequestHandler<Map<String, Object>, Void> {

    private final SqsClient sqsClient;
    private final String queueUrl;
    private final ObjectMapper objectMapper;

    public StockProducerHandler() {
        sqsClient = SqsClient.create();
        objectMapper = new ObjectMapper();
        queueUrl = System.getenv("STOCK_UPDATE_QUEUE_URL");
    }

    @Override
    public Void handleRequest(Map<String, Object> event, Context context) {
        List<StockUpdate> stocks = List.of(
                new StockUpdate(
                        "AAPL",
                        "Apple Inc.",
                        new BigDecimal("250.50"),
                        new BigDecimal("31.2"),
                        new BigDecimal("3800000000000"),
                        "Technology"
                ),
                new StockUpdate(
                        "MSFT",
                        "Microsoft Corporation",
                        new BigDecimal("510.20"),
                        new BigDecimal("35.8"),
                        new BigDecimal("3790000000000"),
                        "Technology"
                ),
                new StockUpdate(
                        "GOOGL",
                        "Alphabet Inc.",
                        new BigDecimal("245.10"),
                        new BigDecimal("28.4"),
                        new BigDecimal("2950000000000"),
                        "Technology"
                )
        );

        for (StockUpdate stockUpdate : stocks) {
            try {
                String body = objectMapper.writeValueAsString(stockUpdate);
                /*SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                                .queueUrl(queueUrl)
                                        .messageBody(body)
                                                .build();*/
                sqsClient.sendMessage(request -> request.queueUrl(queueUrl).messageBody(body));
                context.getLogger().log("Published message with symbol: " + stockUpdate.symbol());
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

        }


        return null;
    }
}
