package com.gemstore.backend.dtos.prediction;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GemPriceHealthDto {

    private String status;

    @JsonProperty("model_loaded")
    private Boolean modelLoaded;

    @JsonProperty("model_version")
    private String modelVersion;

    @JsonProperty("features_count")
    private Integer featuresCount;
}