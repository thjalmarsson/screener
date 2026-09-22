#!/usr/bin/env bash

#Shellsrcipt to generate some base stocks so
#we do not have to redo this process every time we start
#with a new table.

set -e

ENDPOINT="http://localhost:4566"
REGION="eu-west-1"
PROFILE="localstack"
TABLE="Stocks"

put_stock() {
    SYMBOL=$1
    NAME=$2
    PRICE=$3
    PE_RATIO=$4
    MARKET_CAP=$5
    SECTOR=$6

    echo "Adding $SYMBOL..."

    aws dynamodb put-item \
        --table-name "$TABLE" \
        --item "{
            \"symbol\": {\"S\": \"$SYMBOL\"},
            \"name\": {\"S\": \"$NAME\"},
            \"price\": {\"N\": \"$PRICE\"},
            \"peRatio\": {\"N\": \"$PE_RATIO\"},
            \"marketCap\": {\"N\": \"$MARKET_CAP\"},
            \"sector\": {\"S\": \"$SECTOR\"},
            \"screeningGroup\": {\"S\": \"ALL\"}
        }" \
        --profile "$PROFILE" \
        --endpoint-url "$ENDPOINT" \
        --region "$REGION"
}

put_stock "AAPL" "Apple Inc."       "255.00" "34.5" "3800000000000" "Technology"
put_stock "MSFT" "Microsoft Corp."  "520.00" "37.2" "3860000000000" "Technology"
put_stock "GOOGL" "Alphabet Inc."   "245.00" "28.4" "3000000000000" "Technology"
put_stock "AMZN" "Amazon.com Inc."  "230.00" "33.1" "2450000000000" "Consumer"
put_stock "META" "Meta Platforms"   "760.00" "29.7" "1900000000000" "Technology"
put_stock "JPM" "JPMorgan Chase"    "310.00" "15.2" "850000000000"  "Financials"
put_stock "XOM" "Exxon Mobil"       "120.00" "16.8" "480000000000"  "Energy"
put_stock "WMT" "Walmart Inc."      "105.00" "38.5" "840000000000"  "Consumer"
put_stock "KO" "Coca-Cola Co."      "75.00"  "25.3" "320000000000"  "Consumer"
put_stock "VZ" "Verizon"            "45.00"  "10.4" "190000000000"  "Telecom"

echo "Done."