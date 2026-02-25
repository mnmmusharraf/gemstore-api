package com.gemstore.backend.dtos.prediction;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GemPriceResponseDto {

    @JsonProperty("predicted_price_lkr")
    private Double predictedPriceLkr;

    @JsonProperty("predicted_price_usd")
    private Double predictedPriceUsd;

    @JsonProperty("price_range_low_lkr")
    private Double priceRangeLowLkr;

    @JsonProperty("price_range_high_lkr")
    private Double priceRangeHighLkr;

    private String confidence;

    @JsonProperty("quality_grade")
    private String qualityGrade;

    @JsonProperty("gem_summary")
    private GemSummary gemSummary;

    @JsonProperty("price_factors")
    private Map<String, String> priceFactors;

    private List<String> warnings;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GemSummary {
        private String type;
        private String weight;
        private String origin;
        private String treatment;
        @JsonProperty("color_quality")
        private String colorQuality;
        private String clarity;
    }
}