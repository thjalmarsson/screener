# Screener Infrastructure

AWS infrastructure for the Screener project.

The infrastructure is defined using **AWS CDK with Java** and deployed locally using **LocalStack**.

## Prerequisites

The following tools should be installed:

* Java 25
* Maven
* Docker / Docker Compose
* AWS CLI
* AWS CDK
* LocalStack CLI (`lstk`)

The LocalStack container is managed from the root of the repository.

---

## Start LocalStack

From the repository root:

```bash
docker compose up -d
```

Check that the container is running:

```bash
docker compose ps
```

View LocalStack logs:

```bash
docker compose logs -f localstack
```

Stop LocalStack:

```bash
docker compose down
```

---

## AWS LocalStack Profile

The project uses an AWS CLI profile called `localstack`.

Configure it with:

```bash
aws configure --profile localstack
```

Suggested values:

```text
AWS Access Key ID: test
AWS Secret Access Key: test
Default region name: eu-west-1
Default output format: json
```

LocalStack does not require real AWS credentials.

Test connectivity:

```bash
aws sts get-caller-identity \
  --profile localstack \
  --endpoint-url=http://localhost:4566 \
  --region eu-west-1
```

---

# CDK

Run the following commands from:

```bash
cd infrastructure
```

## Build

Compile and test the infrastructure project:

```bash
mvn clean package
```

## Synthesize

Generate the CloudFormation template without deploying anything:

```bash
lstk cdk --region eu-west-1 synth
```

Save the generated template to a file:

```bash
lstk cdk --region eu-west-1 synth > /tmp/screener-template.yaml
```

This is useful for inspecting what CDK will send to CloudFormation.

For example:

```bash
grep -A 20 -B 5 "PeRatioIndex" /tmp/screener-template.yaml
```

## Bootstrap

Bootstrap the LocalStack CDK environment:

```bash
lstk cdk --region eu-west-1 bootstrap
```

This normally only needs to be done once for a LocalStack environment.

## Diff

Compare the CDK definition with the currently deployed stack:

```bash
lstk cdk --region eu-west-1 diff
```

It is useful to run this before deploying infrastructure changes.

## Deploy

Deploy the infrastructure:

```bash
lstk cdk --region eu-west-1 deploy
```

## Destroy

Destroy the deployed stack:

```bash
lstk cdk --region eu-west-1 destroy
```

Some resources may remain if their CDK/CloudFormation deletion policy is `Retain`.

---

# DynamoDB

LocalStack DynamoDB endpoint:

```text
http://localhost:4566
```

Region:

```text
eu-west-1
```

AWS CLI profile:

```text
localstack
```

## List Tables

```bash
aws dynamodb list-tables \
  --profile localstack \
  --endpoint-url=http://localhost:4566 \
  --region eu-west-1
```

## Describe Stocks Table

```bash
aws dynamodb describe-table \
  --table-name Stocks \
  --profile localstack \
  --endpoint-url=http://localhost:4566 \
  --region eu-west-1
```

## Check Global Secondary Indexes

```bash
aws dynamodb describe-table \
  --table-name Stocks \
  --profile localstack \
  --endpoint-url=http://localhost:4566 \
  --region eu-west-1 \
  --query 'Table.GlobalSecondaryIndexes'
```

The current table should contain:

```text
PeRatioIndex

Partition key: screeningGroup
Sort key:      peRatio
```

## Get Stock by Symbol

```bash
aws dynamodb get-item \
  --table-name Stocks \
  --key '{"symbol":{"S":"AAPL"}}' \
  --profile localstack \
  --endpoint-url=http://localhost:4566 \
  --region eu-west-1
```

## Scan Stocks Table

Useful for development/debugging:

```bash
aws dynamodb scan \
  --table-name Stocks \
  --profile localstack \
  --endpoint-url=http://localhost:4566 \
  --region eu-west-1
```

Avoid relying on `Scan` for application access patterns when a DynamoDB `Query` can be used instead.

## Query Stocks by P/E Ratio

Query the `PeRatioIndex` in ascending P/E order:

```bash
aws dynamodb query \
  --table-name Stocks \
  --index-name PeRatioIndex \
  --key-condition-expression "screeningGroup = :group" \
  --expression-attribute-values '{":group":{"S":"ALL"}}' \
  --scan-index-forward \
  --limit 5 \
  --profile localstack \
  --endpoint-url=http://localhost:4566 \
  --region eu-west-1
```

This returns up to five stocks with the lowest P/E ratios in the `ALL` screening group.

For descending order:

```bash
aws dynamodb query \
  --table-name Stocks \
  --index-name PeRatioIndex \
  --key-condition-expression "screeningGroup = :group" \
  --expression-attribute-values '{":group":{"S":"ALL"}}' \
  --no-scan-index-forward \
  --limit 5 \
  --profile localstack \
  --endpoint-url=http://localhost:4566 \
  --region eu-west-1
```

## Delete Stocks Table Manually

Normally CDK should manage infrastructure resources.

For local troubleshooting only:

```bash
aws dynamodb delete-table \
  --table-name Stocks \
  --profile localstack \
  --endpoint-url=http://localhost:4566 \
  --region eu-west-1
```

---

# Useful Development Workflow

After changing CDK infrastructure code:

```bash
mvn clean package
```

Inspect the proposed changes:

```bash
lstk cdk --region eu-west-1 diff
```

Deploy:

```bash
lstk cdk --region eu-west-1 deploy
```

Verify the resources:

```bash
aws dynamodb list-tables \
  --profile localstack \
  --endpoint-url=http://localhost:4566 \
  --region eu-west-1
```

For DynamoDB-specific changes:

```bash
aws dynamodb describe-table \
  --table-name Stocks \
  --profile localstack \
  --endpoint-url=http://localhost:4566 \
  --region eu-west-1
```

---

# Troubleshooting

## `ResourceNotFoundException: Index not found`

Check whether the index actually exists:

```bash
aws dynamodb describe-table \
  --table-name Stocks \
  --profile localstack \
  --endpoint-url=http://localhost:4566 \
  --region eu-west-1 \
  --query 'Table.GlobalSecondaryIndexes'
```

Then verify that CDK is generating the index:

```bash
lstk cdk --region eu-west-1 synth > /tmp/screener-template.yaml

grep -A 20 -B 5 "PeRatioIndex" /tmp/screener-template.yaml
```

If CDK contains the index but the LocalStack DynamoDB table does not, the local CloudFormation state and DynamoDB resource may be out of sync.

For a development environment where deleting the data is acceptable:

```bash
lstk cdk --region eu-west-1 destroy
```

Check whether the retained table still exists:

```bash
aws dynamodb list-tables \
  --profile localstack \
  --endpoint-url=http://localhost:4566 \
  --region eu-west-1
```

If necessary, delete the retained table:

```bash
aws dynamodb delete-table \
  --table-name Stocks \
  --profile localstack \
  --endpoint-url=http://localhost:4566 \
  --region eu-west-1
```

Then redeploy:

```bash
lstk cdk --region eu-west-1 deploy
```

## Inspect CDK Output

CDK-generated files are written to:

```text
cdk.out/
```

These files are generated artifacts and should not be edited manually.

---

# Current Infrastructure

The current DynamoDB design is:

```text
Stocks
│
├── Primary Key
│   └── symbol (String)
│
└── PeRatioIndex (GSI)
    ├── Partition Key: screeningGroup (String)
    └── Sort Key: peRatio (Number)
```

The `PeRatioIndex` supports the access pattern:

```text
Find stocks in screeningGroup = ALL
        ↓
Order by peRatio
        ↓
Return first N stocks
```

This allows the application to retrieve stocks ordered by P/E ratio using a DynamoDB `Query` rather than scanning and sorting the entire table.
