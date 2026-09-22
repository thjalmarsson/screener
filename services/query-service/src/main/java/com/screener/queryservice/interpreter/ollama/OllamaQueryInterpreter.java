package com.screener.queryservice.interpreter.ollama;

import com.screener.queryservice.interpreter.QueryInterpretation;
import com.screener.queryservice.interpreter.QueryInterpreter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class OllamaQueryInterpreter implements QueryInterpreter {

    private static final String SYSTEM_PROMPT = """
            You translate natural-language stock screening requests
            into JSON.

            Return only JSON.

            The JSON must contain exactly these fields:
            - sortBy
            - sortOrder
            - limit

            Valid sortBy values:
            - PE_RATIO

            Valid order values:
            - ASC
            - DESC

            Examples:

            "Give me the 5 stocks with the lowest P/E ratios"
            {"sortBy":"PE_RATIO","order":"ASC","limit":5}

            "Give me the 3 stocks with the highest P/E ratios"
            {"sortBy":"PE_RATIO","order":"DESC","limit":3}
            """;

    private static final String NEW_SYSTEM_PROMPT = """
        You interpret natural-language stock screening requests.

        The application currently supports screening stocks only by P/E ratio.

        Rules:
        - P/E ratio maps to PE_RATIO.
        - "lowest", "smallest", or similar means ASC.
        - "highest", "largest", or similar means DESC.
        - limit is the number of stocks requested.

        If the user's request can be represented using these capabilities:
        - supported must be true
        - reason must be null
        - populate sortBy, sortOrder and limit

        If the request requires an unsupported metric or cannot be represented:
        - supported must be false
        - explain why in reason
        - sortBy, sortOrder and limit must be null
        """;

    private static final Map<String, Object> QUERY_SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of(
                    "supported", Map.of(
                            "type", "boolean"
                    ),
                    "reason", Map.of(
                            "type", List.of("string", "null")
                    ),
                    "sortBy", Map.of(
                            "type", List.of("string", "null"),
                            "enum", Arrays.asList("PE_RATIO", null)
                    ),
                    "sortOrder", Map.of(
                            "type", List.of("string", "null"),
                            "enum", Arrays.asList("ASC", "DESC", null)
                    ),
                    "limit", Map.of(
                            "type", List.of("integer", "null"),
                            "minimum", 1,
                            "maximum", 100
                    )
            ),
            "required", List.of(
                    "supported",
                    "reason",
                    "sortBy",
                    "sortOrder",
                    "limit"
            ),
            "additionalProperties", false
    );

    private final RestClient ollamaClient;
    private final ObjectMapper objectMapper;

    public OllamaQueryInterpreter(RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        this.ollamaClient = restClientBuilder.baseUrl("http://localhost:11434").build();
        this.objectMapper = objectMapper;
    }

    @Override
    public QueryInterpretation interpret(String query) {

        OllamaRequest request = new OllamaRequest(
                "llama3.2:3b",
                false,
                List.of(
                        new OllamaMessage("system", NEW_SYSTEM_PROMPT),
                        new OllamaMessage("user", query)
                ),
                QUERY_SCHEMA
        );

        OllamaResponse response = ollamaClient.post()
                .uri("/api/chat")
                .body(request)
                .retrieve()
                .body(OllamaResponse.class);

        if (response == null || response.message() == null) {
            throw new IllegalStateException(
                    "Ollama returned an empty response"
            );
        }

        return objectMapper.readValue(
                response.message().content(),
                QueryInterpretation.class
        );
    }
}
