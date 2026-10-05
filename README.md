# Screener

Stock screening application built as an AWS microservices learning project.

## Technology

- Java 25
- Maven
- Spring Boot
- AWS
- LocalStack
- DynamoDB
- Local LLM

## Local development

### Start and deploy the backend

Prerequisites: Java 25, Maven, Docker Compose, AWS CLI v2, AWS CDK,
and the LocalStack CLI (`lstk`). The optional natural-language query service
also needs Ollama. Set `LOCALSTACK_AUTH_TOKEN` in the root `.env` file for
the Compose configuration.

Run these commands from the repository root in Bash. Configure the local
profile once (these are dummy credentials):

```bash
aws configure set aws_access_key_id test --profile localstack
aws configure set aws_secret_access_key test --profile localstack
aws configure set region eu-west-1 --profile localstack
aws configure set output json --profile localstack

docker compose up -d
curl --fail http://localhost:4566/_localstack/health
```

Wait for LocalStack to be ready before deploying. Build the Lambda JARs before
CDK loads them, then bootstrap and deploy the stack:

```bash
mvn -pl services/stock-lambda,services/stock-ingestion-lambda,services/stock-producer-lambda -am clean package
(
  cd infrastructure
  lstk cdk --region eu-west-1 bootstrap
  lstk cdk --region eu-west-1 deploy InfrastructureStack
)
```

Bootstrap is only needed for a fresh LocalStack environment. Repeat the build
and deploy after changing Lambda code. The deployed Lambdas run in LocalStack;
they do not need separate Maven startup commands.

### Upload stock data to S3 and test ingestion

The pipeline is S3 → producer Lambda → SQS → ingestion Lambda → DynamoDB.
**Current limitation:** the producer reads top-level `bucket` and `key` fields,
but S3 notifications use a `Records` array. Uploading alone currently triggers
a failing invocation. Until the handler supports S3 events, use the manual
invocation below after uploading to test the rest of the pipeline.

In the same Bash terminal, define a helper that always targets LocalStack and
look up the generated resource names:

```bash
aws_local() {
  aws --profile localstack --endpoint-url http://localhost:4566 --region eu-west-1 "$@"
}

STOCK_BUCKET=$(aws_local cloudformation list-stack-resources \
  --stack-name InfrastructureStack \
  --query "StackResourceSummaries[?ResourceType=='AWS::S3::Bucket' && starts_with(LogicalResourceId, 'StockInputBucket')].PhysicalResourceId | [0]" \
  --output text)

PRODUCER_FUNCTION=$(aws_local cloudformation list-stack-resources \
  --stack-name InfrastructureStack \
  --query "StackResourceSummaries[?ResourceType=='AWS::Lambda::Function' && starts_with(LogicalResourceId, 'StockProducerFunction')].PhysicalResourceId | [0]" \
  --output text)

printf 'Bucket: %s\nProducer: %s\n' "$STOCK_BUCKET" "$PRODUCER_FUNCTION"
```

Both values should be resource names, not `None`. Create a JSON **array** of
stocks with all six fields, then upload it:

```bash
cat > /tmp/screener-stocks.json <<'JSON'
[
  {
    "symbol": "S3TEST",
    "name": "S3 Upload Test",
    "price": 100.50,
    "peRatio": 12.3,
    "marketCap": 1000000000,
    "sector": "Technology"
  }
]
JSON

aws_local s3 cp /tmp/screener-stocks.json "s3://$STOCK_BUCKET/uploads/screener-stocks.json"
aws_local s3 ls "s3://$STOCK_BUCKET/uploads/"

aws_local lambda invoke \
  --function-name "$PRODUCER_FUNCTION" \
  --cli-binary-format raw-in-base64-out \
  --payload "{\"bucket\":\"$STOCK_BUCKET\",\"key\":\"uploads/screener-stocks.json\"}" \
  /tmp/screener-producer-response.json

cat /tmp/screener-producer-response.json
```

The invocation should report `StatusCode: 200` without `FunctionError`; a
successful void handler returns `null` in the response file. A status of 200
alone does not prove the handler succeeded.

SQS ingestion is asynchronous. After a few seconds, check DynamoDB (repeat if
the item has not appeared yet):

```bash
aws_local dynamodb get-item \
  --table-name Stocks \
  --key '{"symbol":{"S":"S3TEST"}}' \
  --consistent-read
```

Expect an `Item` containing `symbol=S3TEST`, `peRatio=12.3`, and
`screeningGroup=ALL`. To verify another upload, change `price` in the JSON,
repeat the upload and manual invocation, and check the updated value.

If ingestion fails, inspect the logs:

```bash
aws_local logs tail "/aws/lambda/$PRODUCER_FUNCTION" --since 10m

INGESTION_FUNCTION=$(aws_local cloudformation list-stack-resources \
  --stack-name InfrastructureStack \
  --query "StackResourceSummaries[?ResourceType=='AWS::Lambda::Function' && starts_with(LogicalResourceId, 'StockIngestionFunction')].PhysicalResourceId | [0]" \
  --output text)
aws_local logs tail "/aws/lambda/$INGESTION_FUNCTION" --since 10m
docker compose logs --tail=100 localstack
```

Producer errors from automatic S3 notifications are expected with the current
handler, even when the manual invocation succeeds.

### Test the stock API and start the query service

Discover the deployed API ID in the terminal where `aws_local` is defined:

```bash
STOCK_API_ID=$(aws_local cloudformation list-stack-resources \
  --stack-name InfrastructureStack \
  --query "StackResourceSummaries[?ResourceType=='AWS::ApiGateway::RestApi'].PhysicalResourceId | [0]" \
  --output text)
export STOCK_SERVICE_BASE_URL="http://localhost:4566/restapis/$STOCK_API_ID/prod/_user_request_"

curl --fail-with-body "$STOCK_SERVICE_BASE_URL/stocks/screen" \
  -H 'Content-Type: application/json' \
  -d '{"sortBy":"PE_RATIO","sortOrder":"ASC","limit":5}'
```

This returns up to five stocks ordered by P/E ratio and does not need Ollama.
On an otherwise empty table, it should include `S3TEST`.

For natural-language queries, start Ollama in a separate terminal if it is
not already running:

```bash
ollama serve
```

Back in the terminal with `STOCK_SERVICE_BASE_URL`, run:

```bash
ollama pull llama3.2:3b
mvn -f services/query-service/pom.xml spring-boot:run
```

The environment variable overrides the hard-coded API ID in
`application.properties`. Keep the service running and test it from another
terminal:

```bash
curl --fail-with-body http://localhost:8081/query \
  -H 'Content-Type: application/json' \
  -d '{"query":"Give me the 5 stocks with the lowest P/E ratios"}'
```

Stop the query service with Ctrl+C and LocalStack with `docker compose down`.
See [the infrastructure guide](infrastructure/README.md) for more CDK and
DynamoDB commands.

### Other local commands

Start AWS services:

    docker compose up -d

Stop AWS services:

    docker compose down

Check LocalStack:

    curl http://localhost:4566/_localstack/health

OLLAMA (or whatever model we are using):

    ollama run llama3.2:3b

Or if using curl: <br/>

    curl http://localhost:11434/api/chat \
    -H "Content-Type: application/json" \
    -d '{
    "model": "llama3.2:3b",
    "stream": false,
    "messages": [
    {
    "role": "system",
    "content": "content"
    },
    {
    "role": "user",
    "content": "content"
    }
    ]
    }'
