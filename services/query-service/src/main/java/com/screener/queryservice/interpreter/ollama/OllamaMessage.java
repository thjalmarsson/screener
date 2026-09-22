package com.screener.queryservice.interpreter.ollama;

public record OllamaMessage(
        String role,
        String content
) {
}
