package com.gemstore.backend.services.prediction;

import com.gemstore.backend.dtos.prediction.GemPriceHealthDto;
import com.gemstore.backend.dtos.prediction.GemPriceRequestDto;
import com.gemstore.backend.dtos.prediction.GemPriceResponseDto;
import com.gemstore.backend.exceptions.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class GemPriceService {

    private final WebClient gemPriceApiClient;

    @Value("${gem-price-api.predict-endpoint:/predict}")
    private String predictEndpoint;

    @Value("${gem-price-api.health-endpoint:/health}")
    private String healthEndpoint;

    @Value("${gem-price-api.enabled:true}")
    private boolean apiEnabled;

    /**
     * Get price prediction for a gem
     */
    public GemPriceResponseDto predictPrice(GemPriceRequestDto request) {
        if (!apiEnabled) {
            throw new BadRequestException("Gem price prediction service is currently disabled");
        }

        log.info("Requesting price prediction for {} {} {}ct",
                request.getColorQuality(), request.getGemColor(), request.getCaratWeight());

        try {
            GemPriceResponseDto response = gemPriceApiClient
                    .post()
                    .uri(predictEndpoint)
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> {
                                        log.error("API error: {}", errorBody);
                                        return Mono.error(new BadRequestException(
                                                "Price prediction failed: " + errorBody));
                                    }))
                    .bodyToMono(GemPriceResponseDto.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            log.info("Price prediction successful: {} LKR",
                    response != null ? response.getPredictedPriceLkr() : "null");

            return response;

        } catch (WebClientResponseException e) {
            log.error("WebClient error: {} - {}", e.getStatusCode(), e.getMessage());
            throw new BadRequestException("Price prediction service error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during price prediction: {}", e.getMessage());
            throw new BadRequestException("Unable to get price prediction. Please try again later.");
        }
    }

    /**
     * Get price prediction asynchronously (non-blocking)
     */
    public Mono<GemPriceResponseDto> predictPriceAsync(GemPriceRequestDto request) {
        if (!apiEnabled) {
            return Mono.error(new BadRequestException("Gem price prediction service is disabled"));
        }

        return gemPriceApiClient
                .post()
                .uri(predictEndpoint)
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> Mono.error(
                                        new BadRequestException("Price prediction failed: " + errorBody))))
                .bodyToMono(GemPriceResponseDto.class)
                .timeout(Duration.ofSeconds(30))
                .doOnSuccess(response -> log.info("Async price prediction: {} LKR",
                        response.getPredictedPriceLkr()))
                .doOnError(error -> log.error("Async prediction error: {}", error.getMessage()));
    }

    /**
     * Check health of the price prediction API
     */
    public GemPriceHealthDto checkHealth() {
        try {
            return gemPriceApiClient
                    .get()
                    .uri(healthEndpoint)
                    .retrieve()
                    .bodyToMono(GemPriceHealthDto.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();
        } catch (Exception e) {
            log.error("Health check failed: {}", e.getMessage());
            GemPriceHealthDto unhealthy = new GemPriceHealthDto();
            unhealthy.setStatus("unhealthy");
            unhealthy.setModelLoaded(false);
            return unhealthy;
        }
    }

    /**
     * Check if the service is available
     */
    public boolean isServiceAvailable() {
        try {
            GemPriceHealthDto health = checkHealth();
            return "healthy".equals(health.getStatus()) && Boolean.TRUE.equals(health.getModelLoaded());
        } catch (Exception e) {
            return false;
        }
    }
}