package com.gemstore.backend.services.prediction;

import com.gemstore.backend.dtos.prediction.GemPriceHealthDto;
import com.gemstore.backend.dtos.prediction.GemPriceRequestDto;
import com.gemstore.backend.dtos.prediction.GemPriceResponseDto;
import com.gemstore.backend.exceptions.BadRequestException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.*;

@DisplayName("GemPriceService - Unit Tests")
class GemPriceServiceTest {

    private MockWebServer mockWebServer;
    private GemPriceService gemPriceService;

    @BeforeEach
    void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        WebClient webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .build();

        gemPriceService = new GemPriceService(webClient);

        setField("predictEndpoint", "/predict");
        setField("healthEndpoint", "/health");
        setField("apiEnabled", true);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    private void setField(String fieldName, Object value) throws Exception {
        Field field = GemPriceService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(gemPriceService, value);
    }

    private GemPriceRequestDto buildRequest() {
        return GemPriceRequestDto.builder()
                .gemType("sapphire")
                .caratWeight(2.5)
                .gemColor("Blue")
                .colorQuality("vivid")
                .clarityScore(4)
                .cutGradeScore(5)
                .shape("oval")
                .origin("sri lanka")
                .treatment("Unheated")
                .x(8.0)
                .y(6.0)
                .z(4.5)
                .build();
    }

    private void pointServiceToDeadUrl() throws Exception {
        WebClient deadClient = WebClient.builder()
                .baseUrl("http://localhost:1")
                .build();
        gemPriceService = new GemPriceService(deadClient);
        setField("predictEndpoint", "/predict");
        setField("healthEndpoint", "/health");
        setField("apiEnabled", true);
    }

    private static final String VALID_PREDICTION_RESPONSE = """
            {
                "predicted_price_lkr": 150000.0,
                "predicted_price_usd": 500.0,
                "price_range_low_lkr": 120000.0,
                "price_range_high_lkr": 180000.0,
                "confidence": "high",
                "quality_grade": "Premium",
                "gem_summary": {
                    "type": "sapphire",
                    "weight": "2.5ct",
                    "origin": "Sri Lanka",
                    "treatment": "Unheated",
                    "color_quality": "Vivid Blue",
                    "clarity": "Eye Clean"
                },
                "price_factors": {
                    "origin": "Premium origin adds value",
                    "treatment": "Unheated commands higher price"
                },
                "warnings": []
            }
            """;

    private static final String HEALTHY_RESPONSE = """
            {
                "status": "healthy",
                "model_loaded": true,
                "model_version": "1.0.0",
                "features_count": 12
            }
            """;

    // ==================== predictPrice ====================

    @Test
    @DisplayName("predictPrice: valid request returns full prediction response")
    void predictPrice_validRequest_returnsPrediction() {
        mockWebServer.enqueue(new MockResponse()
                .setBody(VALID_PREDICTION_RESPONSE)
                .addHeader("Content-Type", "application/json"));

        GemPriceResponseDto response = gemPriceService.predictPrice(buildRequest());

        assertThat(response).isNotNull();
        assertThat(response.getPredictedPriceLkr()).isEqualTo(150000.0);
        assertThat(response.getPredictedPriceUsd()).isEqualTo(500.0);
        assertThat(response.getPriceRangeLowLkr()).isEqualTo(120000.0);
        assertThat(response.getPriceRangeHighLkr()).isEqualTo(180000.0);
        assertThat(response.getConfidence()).isEqualTo("high");
        assertThat(response.getQualityGrade()).isEqualTo("Premium");
        assertThat(response.getGemSummary()).isNotNull();
        assertThat(response.getGemSummary().getType()).isEqualTo("sapphire");
        assertThat(response.getGemSummary().getOrigin()).isEqualTo("Sri Lanka");
        assertThat(response.getPriceFactors()).isNotNull();
        assertThat(response.getPriceFactors()).containsKey("origin");
    }

    @Test
    @DisplayName("predictPrice: API disabled throws BadRequestException")
    void predictPrice_apiDisabled_throwsBadRequest() throws Exception {
        setField("apiEnabled", false);

        assertThatThrownBy(() -> gemPriceService.predictPrice(buildRequest()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("disabled");
    }

    @Test
    @DisplayName("predictPrice: API returns 400 throws BadRequestException")
    void predictPrice_api400_throwsBadRequest() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(400)
                .setBody("Invalid gem parameters")
                .addHeader("Content-Type", "text/plain"));

        assertThatThrownBy(() -> gemPriceService.predictPrice(buildRequest()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("predictPrice: API returns 500 throws BadRequestException")
    void predictPrice_api500_throwsBadRequest() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error")
                .addHeader("Content-Type", "text/plain"));

        assertThatThrownBy(() -> gemPriceService.predictPrice(buildRequest()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("predictPrice: API returns 404 throws BadRequestException")
    void predictPrice_api404_throwsBadRequest() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(404)
                .setBody("Not Found")
                .addHeader("Content-Type", "text/plain"));

        assertThatThrownBy(() -> gemPriceService.predictPrice(buildRequest()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("predictPrice: connection failure throws BadRequestException")
    void predictPrice_connectionFailure_throwsBadRequest() throws Exception {
        mockWebServer.shutdown();
        pointServiceToDeadUrl();

        assertThatThrownBy(() -> gemPriceService.predictPrice(buildRequest()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Unable to get price prediction");
    }

    // ==================== predictPriceAsync ====================

    @Test
    @DisplayName("predictPriceAsync: valid request returns prediction reactively")
    void predictPriceAsync_validRequest_returnsPrediction() {
        mockWebServer.enqueue(new MockResponse()
                .setBody(VALID_PREDICTION_RESPONSE)
                .addHeader("Content-Type", "application/json"));

        GemPriceResponseDto response = gemPriceService.predictPriceAsync(buildRequest()).block();

        assertThat(response).isNotNull();
        assertThat(response.getPredictedPriceLkr()).isEqualTo(150000.0);
        assertThat(response.getGemSummary()).isNotNull();
        assertThat(response.getGemSummary().getTreatment()).isEqualTo("Unheated");
    }

    @Test
    @DisplayName("predictPriceAsync: API disabled returns error Mono")
    void predictPriceAsync_apiDisabled_returnsErrorMono() throws Exception {
        setField("apiEnabled", false);

        assertThatThrownBy(() -> gemPriceService.predictPriceAsync(buildRequest()).block())
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("disabled");
    }

    @Test
    @DisplayName("predictPriceAsync: API error returns error Mono")
    void predictPriceAsync_apiError_returnsErrorMono() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("Server Error")
                .addHeader("Content-Type", "text/plain"));

        assertThatThrownBy(() -> gemPriceService.predictPriceAsync(buildRequest()).block())
                .isInstanceOf(BadRequestException.class);
    }

    // ==================== checkHealth ====================

    @Test
    @DisplayName("checkHealth: healthy API returns full health info")
    void checkHealth_healthyApi_returnsHealthy() {
        mockWebServer.enqueue(new MockResponse()
                .setBody(HEALTHY_RESPONSE)
                .addHeader("Content-Type", "application/json"));

        GemPriceHealthDto health = gemPriceService.checkHealth();

        assertThat(health).isNotNull();
        assertThat(health.getStatus()).isEqualTo("healthy");
        assertThat(health.getModelLoaded()).isTrue();
        assertThat(health.getModelVersion()).isEqualTo("1.0.0");
        assertThat(health.getFeaturesCount()).isEqualTo(12);
    }

    @Test
    @DisplayName("checkHealth: API down returns unhealthy fallback")
    void checkHealth_apiDown_returnsUnhealthy() throws Exception {
        mockWebServer.shutdown();
        pointServiceToDeadUrl();

        GemPriceHealthDto health = gemPriceService.checkHealth();

        assertThat(health).isNotNull();
        assertThat(health.getStatus()).isEqualTo("unhealthy");
        assertThat(health.getModelLoaded()).isFalse();
        assertThat(health.getModelVersion()).isNull();
        assertThat(health.getFeaturesCount()).isNull();
    }

    @Test
    @DisplayName("checkHealth: API returns 500 returns unhealthy fallback")
    void checkHealth_api500_returnsUnhealthy() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("Internal Error")
                .addHeader("Content-Type", "text/plain"));

        GemPriceHealthDto health = gemPriceService.checkHealth();

        assertThat(health).isNotNull();
        assertThat(health.getStatus()).isEqualTo("unhealthy");
        assertThat(health.getModelLoaded()).isFalse();
    }

    // ==================== isServiceAvailable ====================

    @Test
    @DisplayName("isServiceAvailable: healthy + model loaded returns true")
    void isServiceAvailable_healthyWithModel_returnsTrue() {
        mockWebServer.enqueue(new MockResponse()
                .setBody(HEALTHY_RESPONSE)
                .addHeader("Content-Type", "application/json"));

        assertThat(gemPriceService.isServiceAvailable()).isTrue();
    }

    @Test
    @DisplayName("isServiceAvailable: healthy but model NOT loaded returns false")
    void isServiceAvailable_healthyNoModel_returnsFalse() {
        mockWebServer.enqueue(new MockResponse()
                .setBody("""
                        {
                            "status": "healthy",
                            "model_loaded": false,
                            "model_version": null,
                            "features_count": 0
                        }
                        """)
                .addHeader("Content-Type", "application/json"));

        assertThat(gemPriceService.isServiceAvailable()).isFalse();
    }

    @Test
    @DisplayName("isServiceAvailable: unhealthy status returns false")
    void isServiceAvailable_unhealthy_returnsFalse() {
        mockWebServer.enqueue(new MockResponse()
                .setBody("""
                        {
                            "status": "unhealthy",
                            "model_loaded": false,
                            "model_version": null,
                            "features_count": null
                        }
                        """)
                .addHeader("Content-Type", "application/json"));

        assertThat(gemPriceService.isServiceAvailable()).isFalse();
    }

    @Test
    @DisplayName("isServiceAvailable: connection failure returns false")
    void isServiceAvailable_connectionFailure_returnsFalse() throws Exception {
        mockWebServer.shutdown();
        pointServiceToDeadUrl();

        assertThat(gemPriceService.isServiceAvailable()).isFalse();
    }
}