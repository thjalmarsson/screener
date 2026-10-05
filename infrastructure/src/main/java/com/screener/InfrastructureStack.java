package com.screener;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.services.apigateway.LambdaIntegration;
import software.amazon.awscdk.services.apigateway.LambdaIntegrationOptions;
import software.amazon.awscdk.services.apigateway.Resource;
import software.amazon.awscdk.services.apigateway.RestApi;
import software.amazon.awscdk.services.dynamodb.*;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.IEventSource;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.eventsources.S3EventSource;
import software.amazon.awscdk.services.lambda.eventsources.SqsEventSource;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.EventType;
import software.amazon.awscdk.services.sqs.DeadLetterQueue;
import software.amazon.awscdk.services.sqs.Queue;
import software.constructs.Construct;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;

import java.util.List;
import java.util.Map;
// import software.amazon.awscdk.Duration;
// import software.amazon.awscdk.services.sqs.Queue;


public class InfrastructureStack extends Stack {
    public InfrastructureStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public InfrastructureStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        // DynamoDB table for storing the querys.
        Table stocksTable = Table.Builder.create(this, "StocksTable")
                .tableName("Stocks")
                .partitionKey(
                        Attribute.builder()
                                .name("symbol")
                                .type(AttributeType.STRING)
                                .build()
                )
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .build();

        // GSI: Creating to be able to get
        // get stocks by peRatio instead of scanning.
        stocksTable.addGlobalSecondaryIndex(
                GlobalSecondaryIndexProps.builder()
                        .indexName("PeRatioIndex")
                        .partitionKey(Attribute.builder()
                                .name("screeningGroup")
                                .type(AttributeType.STRING)
                                .build()
                        )
                        .sortKey(
                                Attribute.builder()
                                        .name("peRatio")
                                        .type(AttributeType.NUMBER)
                                        .build()
                        )
                        .projectionType(ProjectionType.ALL)
                        .build()
        );

        // Function for retrieving stocks from the DynamoDB
        Function stockFunction = Function.Builder.create(this, "StockFunction")
                .runtime(Runtime.JAVA_25)
                .handler("com.screener.stocklambda.StockLambdaHandler::handleRequest")
                .code(Code.fromAsset("../services/stock-lambda/target/stock-lambda-1.0-SNAPSHOT.jar"))
                .timeout(Duration.seconds(15))
                .build();

        stocksTable.grantReadData(stockFunction);


        // Create the API for the stockLambda and its resources.
        RestApi stockApi = RestApi.Builder
                .create(this, "StockApi")
                .restApiName("restApiName")
                .build();

        Resource stocksResource = stockApi.getRoot().addResource("stocks");
        Resource screenResource = stocksResource.addResource("screen");

        LambdaIntegration stockIntegration = new LambdaIntegration(
                stockFunction, LambdaIntegrationOptions.builder().proxy(true).build());

        screenResource.addMethod("POST", stockIntegration);

        // Ingests into Dynamo
        Function stockIngestionFunction = Function.Builder.create(this, "StockIngestionFunction")
                .runtime(Runtime.JAVA_25)
                .handler("com.screener.stockingestion.StockIngestionHandler::handleRequest")
                .code(Code.fromAsset("../services/stock-ingestion-lambda/target/stock-ingestion-lambda-1.0-SNAPSHOT.jar"))
                .timeout(Duration.seconds(15))
                .build();



        // DLQ and SQS for the decoupling between ingestion/producer
        Queue stockUpdateDlq = Queue.Builder
                .create(this, "StockUpdateDlq")
                .queueName("stock-update-dlq")
                .build();

        Queue stockUpdateQueue = Queue.Builder.create(this, "StockUpdateQueue")
                .queueName("stock-update-queue")
                .visibilityTimeout(Duration.seconds(30))
                .deadLetterQueue(DeadLetterQueue.builder().maxReceiveCount(3).queue(stockUpdateDlq).build())
                .build();

        // Database permissions for ingestion lambda
        stocksTable.grantWriteData(stockIngestionFunction);
        stockIngestionFunction.addEventSource(SqsEventSource.Builder.create(stockUpdateQueue).batchSize(5).build());


        // Read from S3
        Function stockProducerFunction = Function.Builder.create(this, "StockProducerFunction")
                .runtime(Runtime.JAVA_25)
                .handler("com.screener.stockproducer.StockProducerHandler::handleRequest")
                .code(Code.fromAsset("../services/stock-producer-lambda/target/stock-producer-lambda-1.0-SNAPSHOT.jar"))
                .timeout(Duration.seconds(15))
                .environment(Map.of("STOCK_UPDATE_QUEUE_URL", stockUpdateQueue.getQueueUrl()))
                .build();

        stockUpdateQueue.grantSendMessages(stockProducerFunction);

        Bucket stockBucket = Bucket.Builder.create(this, "StockInputBucket")
                .build();

        stockBucket.grantRead(stockProducerFunction);
        stockProducerFunction.addEventSource(S3EventSource.Builder.create(stockBucket).events(List.of(EventType.OBJECT_CREATED)).build());
    }
}
