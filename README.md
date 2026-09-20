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

Start AWS services:

    docker compose up -d

Stop AWS services:

    docker compose down

Check LocalStack:

    curl http://localhost:4566/_localstack/health