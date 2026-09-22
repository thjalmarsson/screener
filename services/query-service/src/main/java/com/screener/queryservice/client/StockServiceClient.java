package com.screener.queryservice.client;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class StockServiceClient {

    private final RestClient restClient;

    public StockServiceClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.baseUrl("http://localhost:8080").build();
    }

    public List<StockResponse> screen(StockScreenRequest request) {
        return restClient.post().uri("/stocks/screen")
                .body(request)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
        });
    }
}
