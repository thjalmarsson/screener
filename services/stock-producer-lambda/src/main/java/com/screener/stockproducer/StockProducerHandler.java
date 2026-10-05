package com.screener.stockproducer;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.screener.stockproducer.model.StockUpdate;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class StockProducerHandler implements RequestHandler<S3Event, Void> {

    private final SqsClient sqsClient;
    private final S3Client s3Client;
    private final String queueUrl;
    private final ObjectMapper objectMapper;

    public StockProducerHandler() {
        sqsClient = SqsClient.create();
        s3Client = S3Client.create();
        objectMapper = new ObjectMapper();
        queueUrl = System.getenv("STOCK_UPDATE_QUEUE_URL");
    }

    @Override
    //public Void handleRequest(Map<String, Object> event, Context context) {
    public Void handleRequest(S3Event event, Context context) {
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
                throw new RuntimeException(e);
            }

            for (StockUpdate stockUpdate : stocks) {
                try {
                    String body = objectMapper.writeValueAsString(stockUpdate);
                    sqsClient.sendMessage(request -> request.queueUrl(queueUrl).messageBody(body));
                    context.getLogger().log("Published message with symbol: " + stockUpdate.symbol());
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }

            }


        }
        return null;
    }
}
