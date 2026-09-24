package com.screener.stockingestion;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.screener.stockingestion.model.StockUpdate;
import com.screener.stockingestion.repository.StockRepository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.ArrayList;
import java.util.List;

public class StockIngestionHandler implements RequestHandler<SQSEvent, SQSBatchResponse> {

    private final StockRepository stockRepository;
    private final ObjectMapper objectMapper;

    public StockIngestionHandler() {
        DynamoDbClient dynamoDbClient = DynamoDbClient.create();
        this.stockRepository = new StockRepository(dynamoDbClient);
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public SQSBatchResponse handleRequest(SQSEvent sqsEvent, Context context) {
        List<SQSBatchResponse.BatchItemFailure> failures = new ArrayList<>();

        // TODO: Add multithreading
        for(SQSEvent.SQSMessage message : sqsEvent.getRecords()) {
            context.getLogger().log("Processing SQS message: " + message.getMessageId());

            //warp in try/Catch
            try {
                StockUpdate stockUpdate = objectMapper.readValue(message.getBody(), StockUpdate.class);
                stockRepository.save(stockUpdate);
                context.getLogger().log("Updated Stock:" + stockUpdate.symbol());
            } catch (Exception e) {
                context.getLogger().log("Failed to procecss message with id: " + message.getMessageId() + ", withError:" + e.getMessage());
                failures.add(
                        new SQSBatchResponse.BatchItemFailure(
                                message.getMessageId()
                        ));
            }
        }
        return new SQSBatchResponse(failures);
    }
}
