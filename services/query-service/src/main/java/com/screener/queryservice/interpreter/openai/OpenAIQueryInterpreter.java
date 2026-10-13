package com.screener.queryservice.interpreter.openai;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.StructuredResponse;
import com.openai.models.responses.StructuredResponseCreateParams;
import com.screener.queryservice.interpreter.QueryInterpretation;
import com.screener.queryservice.interpreter.QueryInterpreter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@ConditionalOnProperty(
        prefix = "query.interpreter",
        name = "provider",
        havingValue = "openai"
)
public class OpenAIQueryInterpreter implements QueryInterpreter {

    private final OpenAIClient client;

    private static final String SYSTEM_PROMPT = """
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

    public OpenAIQueryInterpreter(@Value("${openai.api-key}") String apiKey) {
        this.client = OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .build();
    }

    @Override
    public QueryInterpretation interpret(String query) {

        StructuredResponseCreateParams params = ResponseCreateParams.builder()
                .model(ChatModel.GPT_5_2)
                .instructions(SYSTEM_PROMPT)
                .input(query)
                .text(QueryInterpretation.class)
                .build();

        StructuredResponse<QueryInterpretation> response = client.responses().create(params);
        QueryInterpretation queryInterpretation =
                response.output().stream()
                        .flatMap(item -> item.message().stream())
                        .flatMap(message -> message.content().stream())
                        .flatMap(content -> content.outputText().stream())
                        .findFirst()
                        .orElseThrow();
        return queryInterpretation;
    }
}
