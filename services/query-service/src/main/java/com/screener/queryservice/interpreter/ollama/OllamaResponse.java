package com.screener.queryservice.interpreter.ollama;

import java.util.List;

public record OllamaResponse(
        String model,
        String created_at,
        OllamaMessage message,
        boolean done,
        String done_reason,
        long total_duration,
        long load_duration
) {
}
