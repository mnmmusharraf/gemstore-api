package com.gemstore.backend.controllers.prediction;

import com.gemstore.backend.dtos.prediction.GemPriceHealthDto;
import com.gemstore.backend.dtos.prediction.GemPriceRequestDto;
import com.gemstore.backend.dtos.prediction.GemPriceResponseDto;
import com.gemstore.backend.services.prediction.GemPriceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/gems/price")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Gem Price Prediction", description = "AI-powered gem price prediction endpoints")
public class GemPriceController {

    private final GemPriceService gemPriceService;

    @PostMapping("/predict")
    @Operation(summary = "Predict gem price", description = "Get AI-powered price prediction for a gemstone")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Price prediction successful"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "503", description = "Price prediction service unavailable")
    })
    public ResponseEntity<GemPriceResponseDto> predictPrice(
            @Valid @RequestBody GemPriceRequestDto request) {

        log.info("Received price prediction request for {}ct {} {}",
                request.getCaratWeight(), request.getGemColor(), request.getGemType());

        GemPriceResponseDto response = gemPriceService.predictPrice(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/predict/async")
    @Operation(summary = "Predict gem price (async)", description = "Get price prediction asynchronously")
    public Mono<ResponseEntity<GemPriceResponseDto>> predictPriceAsync(
            @Valid @RequestBody GemPriceRequestDto request) {

        return gemPriceService.predictPriceAsync(request)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/health")
    @Operation(summary = "Check service health", description = "Check if price prediction service is available")
    public ResponseEntity<GemPriceHealthDto> checkHealth() {
        GemPriceHealthDto health = gemPriceService.checkHealth();
        return ResponseEntity.ok(health);
    }

    @GetMapping("/status")
    @Operation(summary = "Get service status", description = "Get detailed service status")
    public ResponseEntity<Map<String, Object>> getServiceStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("available", gemPriceService.isServiceAvailable());
        status.put("health", gemPriceService.checkHealth());
        return ResponseEntity.ok(status);
    }

    @GetMapping("/options")
    @Operation(summary = "Get prediction options", description = "Get available options for price prediction")
    public ResponseEntity<Map<String, Object>> getPredictionOptions() {
        Map<String, Object> options = new HashMap<>();

        options.put("gemTypes", List.of("sapphire", "ruby", "emerald", "diamond"));

        options.put("gemColors", Map.of(
                "sapphire", List.of("blue", "pink", "yellow", "white", "orange", "purple", "teal", "padparadscha"),
                "ruby", List.of("red", "pink"),
                "emerald", List.of("green"),
                "diamond", List.of("white", "yellow", "pink", "blue")
        ));

        options.put("colorQualities", List.of("vivid", "royal", "cornflower", "normal", "light"));

        options.put("shapes", List.of("oval", "cushion", "round", "emerald", "pear",
                "heart", "marquise", "princess", "radiant", "asscher"));

        options.put("origins", Map.of(
                "sapphire", List.of("sri lanka", "myanmar", "madagascar", "tanzania", "other"),
                "ruby", List.of("myanmar", "mozambique", "sri lanka", "thailand", "other"),
                "emerald", List.of("colombia", "zambia", "afghanistan", "brazil", "other"),
                "diamond", List.of("south africa", "russia", "botswana", "other")
        ));

        options.put("treatments", List.of("Heated", "Unheated", "Oiled"));

        options.put("clarityScores", Map.of(
                1, "Heavily Included",
                2, "Included",
                3, "Slightly Included",
                4, "Eye Clean",
                5, "Loupe Clean"
        ));

        options.put("cutGradeScores", Map.of(
                1, "Poor",
                2, "Fair",
                3, "Good",
                4, "Very Good",
                5, "Excellent"
        ));

        return ResponseEntity.ok(options);
    }

    @PostMapping("/estimate/quick")
    @Operation(summary = "Quick price estimate", description = "Get a quick price estimate with minimal parameters")
    public ResponseEntity<GemPriceResponseDto> quickEstimate(
            @RequestParam String gemType,
            @RequestParam Double caratWeight,
            @RequestParam String gemColor,
            @RequestParam(defaultValue = "normal") String colorQuality,
            @RequestParam(defaultValue = "sri lanka") String origin,
            @RequestParam(defaultValue = "Heated") String treatment) {

        GemPriceRequestDto request = GemPriceRequestDto.builder()
                .gemType(gemType)
                .caratWeight(caratWeight)
                .gemColor(gemColor)
                .colorQuality(colorQuality)
                .clarityScore(3)  // Default to "Slightly Included"
                .cutGradeScore(3) // Default to "Good"
                .shape("oval")    // Default shape
                .origin(origin)
                .treatment(treatment)
                .build();

        return ResponseEntity.ok(gemPriceService.predictPrice(request));
    }
}