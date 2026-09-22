package com.screener;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.services.apigateway.LambdaIntegration;
import software.amazon.awscdk.services.apigateway.LambdaIntegrationOptions;
import software.amazon.awscdk.services.apigateway.Resource;
import software.amazon.awscdk.services.apigateway.RestApi;
import software.amazon.awscdk.services.dynamodb.*;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.constructs.Construct;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
// import software.amazon.awscdk.Duration;
// import software.amazon.awscdk.services.sqs.Queue;


public class InfrastructureStack extends Stack {
    public InfrastructureStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public InfrastructureStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        // The code that defines your stack goes here

        // example resource
        // final Queue queue = Queue.Builder.create(this, "InfrastructureQueue")
        //         .visibilityTimeout(Duration.seconds(300))
        //         .build();
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

        Function stockFunction = Function.Builder.create(this, "StockFunction")
                .runtime(Runtime.JAVA_25)
                .handler("com.screener.stocklambda.StockLambdaHandler::handleRequest")
                .code(Code.fromAsset("../services/stock-lambda/target/stock-lambda-1.0-SNAPSHOT.jar"))
                .timeout(Duration.seconds(15))
                .build();

        stocksTable.grantReadData(stockFunction);

        RestApi stockApi = RestApi.Builder
                .create(this, "StockApi")
                .restApiName("restApiName")
                .build();

        Resource stocksResource = stockApi.getRoot().addResource("stocks");
        Resource screenResource = stocksResource.addResource("screen");

        LambdaIntegration stockIntegration = new LambdaIntegration(
                stockFunction, LambdaIntegrationOptions.builder().proxy(true).build());

        screenResource.addMethod("POST", stockIntegration);
    }
}
