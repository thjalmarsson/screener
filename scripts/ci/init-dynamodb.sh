#!/usr/bin/env bash
# Run inside a fresh CI LocalStack container. This intentionally fails if the
# table already exists rather than changing an existing development database.
set -euo pipefail

export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_DEFAULT_REGION=eu-west-1

# Keep this schema aligned with infrastructure/InfrastructureStack.java.
awslocal dynamodb create-table \
  --table-name Stocks \
  --billing-mode PAY_PER_REQUEST \
  --attribute-definitions \
    AttributeName=symbol,AttributeType=S \
    AttributeName=screeningGroup,AttributeType=S \
    AttributeName=peRatio,AttributeType=N \
  --key-schema AttributeName=symbol,KeyType=HASH \
  --global-secondary-indexes '[{"IndexName":"PeRatioIndex","KeySchema":[{"AttributeName":"screeningGroup","KeyType":"HASH"},{"AttributeName":"peRatio","KeyType":"RANGE"}],"Projection":{"ProjectionType":"ALL"}}]'

awslocal dynamodb wait table-exists --table-name Stocks

# Fixture required by StockRepositoryIntegrationTest.
awslocal dynamodb put-item --table-name Stocks --item '{
  "symbol": {"S": "AAPL"},
  "name": {"S": "Apple Inc."},
  "price": {"N": "230.00"},
  "peRatio": {"N": "34.5"},
  "marketCap": {"N": "3500000000000"},
  "sector": {"S": "Technology"},
  "screeningGroup": {"S": "ALL"}
}'
