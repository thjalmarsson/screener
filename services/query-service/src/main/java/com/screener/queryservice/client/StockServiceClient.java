package com.screener.queryservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class StockServiceClient {

    private final RestClient restClient;

    public StockServiceClient(RestClient.Builder restClientBuilder,
                              @Value("${stock-service.base-url}") String stockServiceBaseUrl) {
        this.restClient = restClientBuilder.baseUrl(stockServiceBaseUrl).build();
    }

    public List<StockResponse> screen(StockScreenRequest request) {
        return restClient.post().uri("/stocks/screen")
                .body(request)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
        });
    }
}
