package com.screener.stockproducer;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.screener.stockproducer.model.StockMessage;
import com.screener.stockproducer.model.StockUpdate;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.regions.Region;

import java.net.URI;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class StockProducerHandler implements RequestHandler<S3Event, Void> {

    private final SqsClient sqsClient;
    private final S3Client s3Client;
    private final String queueUrl;
    private final ObjectMapper objectMapper;

    public StockProducerHandler() {
        String endpoint = System.getenv("LOCALSTACK_ENDPOINT");
        if (endpoint == null || endpoint.isBlank()) {
            sqsClient = SqsClient.create();
            s3Client = S3Client.create();
        } else {
            URI localstackEndpoint = URI.create(endpoint);
            sqsClient = SqsClient.builder()
                    .endpointOverride(localstackEndpoint)
                    .region(Region.EU_WEST_1)
                    .build();
            s3Client = S3Client.builder()
                    .endpointOverride(localstackEndpoint)
                    .region(Region.EU_WEST_1)
                    .forcePathStyle(true)
                    .build();
        }
        objectMapper = new ObjectMapper();
        queueUrl = System.getenv("STOCK_UPDATE_QUEUE_URL");
    }

    @Override
    public Void handleRequest(S3Event event, Context context) {
        String requestId = context.getAwsRequestId();
        long startedAt = System.currentTimeMillis();
        int filesProcessed = 0;
        int stocksPublished = 0;

        log(context, String.format(
                "{\"event\":\"s3_file_processing_started\",\"request_id\":\"%s\",\"file_count\":%d}",
                requestId,
                event.getRecords().size()
        ));
        for (S3EventNotification.S3EventNotificationRecord record : event.getRecords()) {
            String bucket = record.getS3().getBucket().getName();
            String key = record.getS3().getObject().getUrlDecodedKey();
            String json = s3Client.getObjectAsBytes(request -> request.bucket(bucket).key(key)).asUtf8String();

            StockUpdate[] stocks;
            try {
                stocks = objectMapper.readValue(
                        json,
                        StockUpdate[].class
                );
            } catch (JsonProcessingException e) {
                log(context, String.format(
                        "{\"event\":\"s3_file_processing_failed\",\"request_id\":\"%s\",\"error_type\":%s}",
                        requestId, "JsonProcessingException.StockUpdate[]"
                ));
                throw new RuntimeException(e);
            }

            for (int i = 0; i < stocks.length; i++) {
                try {
                    String body = objectMapper.writeValueAsString(new StockMessage(stocks[i], createTraceId(requestId, stocks[i].symbol(), i)));
                    sqsClient.sendMessage(request -> request.queueUrl(queueUrl).messageBody(body));
                    stocksPublished++;
                    log(context, String.format(
                            "{\"event\":\"s3_file_processing_published\",\"request_id\":\"%s\",\"symbol\":\"%s\", \"tracing_id:\"%s\"}",
                            requestId, createTraceId(requestId, stocks[i].symbol(), i)
                    ));
                } catch (JsonProcessingException e) {
                    log(context, String.format(
                            "{\"event\":\"s3_file_processing_failed\",\"request_id\":\"%s\",\"error_type\":%s}",
                            requestId, "JsonProcessingException"
                    ));
                    throw new RuntimeException(e);
                }

            }
            filesProcessed++;
        }
        long executionTime = System.currentTimeMillis() - startedAt;
        log(context, String.format(
                "{\"event\":\"s3_file_processing_completed\"," +
                        "\"request_id\":\"%s\"," +
                        "\"execution_time_ms\":%d," +
                        "\"files_processed\":%d," +
                        "\"stocks_published\":%d}",
                requestId,
                executionTime,
                filesProcessed,
                stocksPublished
        ));
        return null;
    }

    private void log(Context context, String message) {
        context.getLogger().log(message + System.lineSeparator());
    }

    /*
    * Creates traceId so we can trace the requests for the entire
    * ingestion flow.
    * */
    private String createTraceId(String requestId, String symbol, int index) {
        return String.format("%s-%s-%d", requestId, symbol, index);
    }
}
