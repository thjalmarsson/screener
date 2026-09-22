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