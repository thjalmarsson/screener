package com.screener.queryservice.interpreter.ollama;

import java.util.List;
import java.util.Map;

public record OllamaRequest(
        String model,
        boolean stream,
        List<OllamaMessage> messages,
        Map<String, Object> format
) {
}
