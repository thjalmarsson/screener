package com.screener.stockingestion;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.screener.stockingestion.model.StockMessage;
import com.screener.stockingestion.repository.StockRepository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.regions.Region;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class StockIngestionHandler implements RequestHandler<SQSEvent, SQSBatchResponse> {

    private final StockRepository stockRepository;
    private final ObjectMapper objectMapper;

    public StockIngestionHandler() {
        DynamoDbClient dynamoDbClient = createDynamoDbClient();
        this.stockRepository = new StockRepository(dynamoDbClient);
        this.objectMapper = new ObjectMapper();
    }

    private DynamoDbClient createDynamoDbClient() {
        String endpoint = System.getenv("LOCALSTACK_ENDPOINT");
        if (endpoint == null || endpoint.isBlank()) {
            return DynamoDbClient.create();
        }
        return DynamoDbClient.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.EU_WEST_1)
                .build();
    }

    @Override
    public SQSBatchResponse handleRequest(SQSEvent sqsEvent, Context context) {
        List<SQSBatchResponse.BatchItemFailure> failures = new ArrayList<>();
        int successfullyUpdated = 0;
        // TODO: Add multithreading
        for(SQSEvent.SQSMessage message : sqsEvent.getRecords()) {
            log(context, String.format("{\"event\":\"processing_sqs_event\", \"sqsEventId: \"%s\"}", message.getMessageId()));

            try {
                StockMessage stockMessage = objectMapper.readValue(message.getBody(), StockMessage.class);
                stockRepository.save(stockMessage.stockUpdate());
                log(context, String.format("{\"event\":\"published_to_dynamodb\", \"traceId\":\"%s\", \"messageId\":\"%s\"}", stockMessage.tracingId(), message.getMessageId()));
                successfullyUpdated++;
            } catch (Exception e) {
                log(context, String.format("{\"event\":\"published_to_dynamodb_failure\", \"traceId\":\"%s\"}", message.getMessageId()));
                failures.add(
                        new SQSBatchResponse.BatchItemFailure(
                                message.getMessageId()
                        ));
            }
        }
        log(context, String.format("{\"event\":\"processing_sqs_event_finished\", \"success\":\"%d\", \"failures\":\"%d\"}", successfullyUpdated, failures.size()));
        return new SQSBatchResponse(failures);
    }

    private void log(Context context, String message) {
        context.getLogger().log(message + System.lineSeparator());
    }
}
