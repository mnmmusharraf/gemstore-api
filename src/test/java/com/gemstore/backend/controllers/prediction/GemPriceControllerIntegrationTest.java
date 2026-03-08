package com.gemstore.backend.controllers.prediction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gemstore.backend.dtos.prediction.GemPriceRequestDto;
import com.gemstore.backend.entities.user.User;
import com.gemstore.backend.repositories.user.UserRepository;
import com.gemstore.backend.services.auth.JWTService;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GemPriceControllerIntegrationTest {

    private static final MockWebServer mockWebServer;

    static {
        try {
            mockWebServer = new MockWebServer();
            mockWebServer.start();
        } catch (IOException e) {
            throw new RuntimeException("Failed to start MockWebServer before Spring context", e);
        }
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JWTService jwtService;

    private String userToken;

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

    @BeforeAll
    static void startMockServer() {
        // MockWebServer is already started in the static initializer.
        // Spring context reads mockWebServer.url("/") via @DynamicPropertySource,
        // which is guaranteed to be ready before context initialization.
    }

    @AfterAll
    static void stopMockServer() throws IOException {
        mockWebServer.shutdown();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("gem-price-api.base-url", () -> mockWebServer.url("/").toString());
        registry.add("gem-price-api.predict-endpoint", () -> "/predict");
        registry.add("gem-price-api.health-endpoint", () -> "/health");
        registry.add("gem-price-api.enabled", () -> "true");
    }

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        User user = User.builder()
                .displayName("Test User")
                .email("test@example.com")
                .username("testuser")
                .passwordHash(passwordEncoder.encode("password123"))
                .provider("LOCAL")
                .role("USER")
                .status("ACTIVE")
                .build();
        user = userRepository.save(user);
        userToken = jwtService.generateToken(user.getId(), user.getUsername(), user.getRole());
    }

    private GemPriceRequestDto buildValidRequest() {
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

    // ==================== POST /predict ====================

    @Test
    void predictPrice_validRequest_returnsOk() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setBody(VALID_PREDICTION_RESPONSE)
                .addHeader("Content-Type", "application/json"));

        mockMvc.perform(post("/api/v1/gems/price/predict")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.predicted_price_lkr").value(150000.0))
                .andExpect(jsonPath("$.predicted_price_usd").value(500.0))
                .andExpect(jsonPath("$.price_range_low_lkr").value(120000.0))
                .andExpect(jsonPath("$.price_range_high_lkr").value(180000.0))
                .andExpect(jsonPath("$.confidence").value("high"))
                .andExpect(jsonPath("$.quality_grade").value("Premium"))
                .andExpect(jsonPath("$.gem_summary.type").value("sapphire"))
                .andExpect(jsonPath("$.gem_summary.origin").value("Sri Lanka"));
    }

    @Test
    void predictPrice_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/gems/price/predict")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void predictPrice_missingGemType_returnsBadRequest() throws Exception {
        GemPriceRequestDto request = buildValidRequest();
        request.setGemType(null);

        mockMvc.perform(post("/api/v1/gems/price/predict")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void predictPrice_missingCaratWeight_returnsBadRequest() throws Exception {
        GemPriceRequestDto request = buildValidRequest();
        request.setCaratWeight(null);

        mockMvc.perform(post("/api/v1/gems/price/predict")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void predictPrice_missingGemColor_returnsBadRequest() throws Exception {
        GemPriceRequestDto request = buildValidRequest();
        request.setGemColor(null);

        mockMvc.perform(post("/api/v1/gems/price/predict")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void predictPrice_missingColorQuality_returnsBadRequest() throws Exception {
        GemPriceRequestDto request = buildValidRequest();
        request.setColorQuality(null);

        mockMvc.perform(post("/api/v1/gems/price/predict")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void predictPrice_missingClarityScore_returnsBadRequest() throws Exception {
        GemPriceRequestDto request = buildValidRequest();
        request.setClarityScore(null);

        mockMvc.perform(post("/api/v1/gems/price/predict")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void predictPrice_clarityScoreTooHigh_returnsBadRequest() throws Exception {
        GemPriceRequestDto request = buildValidRequest();
        request.setClarityScore(10);

        mockMvc.perform(post("/api/v1/gems/price/predict")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void predictPrice_clarityScoreTooLow_returnsBadRequest() throws Exception {
        GemPriceRequestDto request = buildValidRequest();
        request.setClarityScore(0);

        mockMvc.perform(post("/api/v1/gems/price/predict")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void predictPrice_cutGradeScoreTooHigh_returnsBadRequest() throws Exception {
        GemPriceRequestDto request = buildValidRequest();
        request.setCutGradeScore(10);

        mockMvc.perform(post("/api/v1/gems/price/predict")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void predictPrice_negativeCaratWeight_returnsBadRequest() throws Exception {
        GemPriceRequestDto request = buildValidRequest();
        request.setCaratWeight(-1.0);

        mockMvc.perform(post("/api/v1/gems/price/predict")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void predictPrice_caratWeightTooLarge_returnsBadRequest() throws Exception {
        GemPriceRequestDto request = buildValidRequest();
        request.setCaratWeight(200.0);

        mockMvc.perform(post("/api/v1/gems/price/predict")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void predictPrice_missingShape_returnsBadRequest() throws Exception {
        GemPriceRequestDto request = buildValidRequest();
        request.setShape(null);

        mockMvc.perform(post("/api/v1/gems/price/predict")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void predictPrice_missingOrigin_returnsBadRequest() throws Exception {
        GemPriceRequestDto request = buildValidRequest();
        request.setOrigin(null);

        mockMvc.perform(post("/api/v1/gems/price/predict")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void predictPrice_missingTreatment_returnsBadRequest() throws Exception {
        GemPriceRequestDto request = buildValidRequest();
        request.setTreatment(null);

        mockMvc.perform(post("/api/v1/gems/price/predict")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void predictPrice_externalApiReturns500_returnsBadRequest() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("Model inference failed")
                .addHeader("Content-Type", "text/plain"));

        mockMvc.perform(post("/api/v1/gems/price/predict")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidRequest())))
                .andExpect(status().isBadRequest());
    }

    // ==================== POST /predict/async ====================

    @WithMockUser(username = "testuser", roles = {"USER"})
    @Test
    void predictPriceAsync_validRequest_returnsOk() throws Exception {

        mockWebServer.enqueue(new MockResponse()
                .setBody(VALID_PREDICTION_RESPONSE)
                .addHeader("Content-Type", "application/json"));

        MvcResult mvcResult = mockMvc.perform(post("/api/v1/gems/price/predict/async")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidRequest())))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.predicted_price_lkr").value(150000.0));
    }

    @Test
    void predictPriceAsync_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/gems/price/predict/async")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidRequest())))
                .andExpect(status().isUnauthorized());
    }

    // ==================== GET /health ====================

    @Test
    void checkHealth_serviceHealthy_returnsOk() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setBody(HEALTHY_RESPONSE)
                .addHeader("Content-Type", "application/json"));

        mockMvc.perform(get("/api/v1/gems/price/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("healthy"))
                .andExpect(jsonPath("$.model_loaded").value(true))
                .andExpect(jsonPath("$.model_version").value("1.0.0"))
                .andExpect(jsonPath("$.features_count").value(12));
    }

    @Test
    void checkHealth_serviceDown_returnsUnhealthyFallback() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("Error")
                .addHeader("Content-Type", "text/plain"));

        mockMvc.perform(get("/api/v1/gems/price/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("unhealthy"))
                .andExpect(jsonPath("$.model_loaded").value(false));
    }

    @Test
    void checkHealth_noAuthRequired_returnsOk() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setBody(HEALTHY_RESPONSE)
                .addHeader("Content-Type", "application/json"));

        mockMvc.perform(get("/api/v1/gems/price/health"))
                .andExpect(status().isOk());
    }

    // ==================== GET /status ====================

    @Test
    void getServiceStatus_returnsAvailability() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setBody(HEALTHY_RESPONSE)
                .addHeader("Content-Type", "application/json"));
        mockWebServer.enqueue(new MockResponse()
                .setBody(HEALTHY_RESPONSE)
                .addHeader("Content-Type", "application/json"));

        mockMvc.perform(get("/api/v1/gems/price/status")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.health.status").value("healthy"));
    }

    @Test
    void getServiceStatus_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/gems/price/status"))
                .andExpect(status().isUnauthorized());
    }

    // ==================== GET /options ====================

    @Test
    void getPredictionOptions_returnsAllOptions() throws Exception {
        mockMvc.perform(get("/api/v1/gems/price/options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gemTypes").isArray())
                .andExpect(jsonPath("$.gemTypes.length()").value(4))
                .andExpect(jsonPath("$.gemColors").isMap())
                .andExpect(jsonPath("$.gemColors.sapphire").isArray())
                .andExpect(jsonPath("$.colorQualities").isArray())
                .andExpect(jsonPath("$.shapes").isArray())
                .andExpect(jsonPath("$.shapes.length()").value(10))
                .andExpect(jsonPath("$.origins").isMap())
                .andExpect(jsonPath("$.origins.sapphire").isArray())
                .andExpect(jsonPath("$.treatments").isArray())
                .andExpect(jsonPath("$.treatments.length()").value(3))
                .andExpect(jsonPath("$.clarityScores").isMap())
                .andExpect(jsonPath("$.cutGradeScores").isMap());
    }

    @Test
    void getPredictionOptions_noAuthRequired_returnsOk() throws Exception {
        mockMvc.perform(get("/api/v1/gems/price/options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gemTypes").isArray());
    }

    // ==================== POST /estimate/quick ====================

    @Test
    void quickEstimate_validParams_returnsOk() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setBody(VALID_PREDICTION_RESPONSE)
                .addHeader("Content-Type", "application/json"));

        mockMvc.perform(post("/api/v1/gems/price/estimate/quick")
                        .header("Authorization", "Bearer " + userToken)
                        .param("gemType", "sapphire")
                        .param("caratWeight", "2.5")
                        .param("gemColor", "Blue")
                        .param("colorQuality", "vivid")
                        .param("origin", "sri lanka")
                        .param("treatment", "Unheated"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.predicted_price_lkr").value(150000.0))
                .andExpect(jsonPath("$.confidence").value("high"));
    }

    @Test
    void quickEstimate_withDefaults_returnsOk() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setBody(VALID_PREDICTION_RESPONSE)
                .addHeader("Content-Type", "application/json"));

        mockMvc.perform(post("/api/v1/gems/price/estimate/quick")
                        .header("Authorization", "Bearer " + userToken)
                        .param("gemType", "ruby")
                        .param("caratWeight", "1.0")
                        .param("gemColor", "red"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.predicted_price_lkr").isNumber());
    }

    @Test
    void quickEstimate_missingGemType_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/gems/price/estimate/quick")
                        .header("Authorization", "Bearer " + userToken)
                        .param("caratWeight", "2.5")
                        .param("gemColor", "Blue"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void quickEstimate_missingCaratWeight_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/gems/price/estimate/quick")
                        .header("Authorization", "Bearer " + userToken)
                        .param("gemType", "sapphire")
                        .param("gemColor", "Blue"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void quickEstimate_missingGemColor_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/gems/price/estimate/quick")
                        .header("Authorization", "Bearer " + userToken)
                        .param("gemType", "sapphire")
                        .param("caratWeight", "2.5"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void quickEstimate_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/gems/price/estimate/quick")
                        .param("gemType", "sapphire")
                        .param("caratWeight", "2.5")
                        .param("gemColor", "Blue"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void quickEstimate_externalApiError_returnsBadRequest() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("Model error")
                .addHeader("Content-Type", "text/plain"));

        mockMvc.perform(post("/api/v1/gems/price/estimate/quick")
                        .header("Authorization", "Bearer " + userToken)
                        .param("gemType", "sapphire")
                        .param("caratWeight", "2.5")
                        .param("gemColor", "Blue"))
                .andExpect(status().isBadRequest());
    }
}