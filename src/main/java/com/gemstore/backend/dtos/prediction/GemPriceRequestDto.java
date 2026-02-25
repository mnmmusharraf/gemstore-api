package com.gemstore.backend.dtos.prediction;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GemPriceRequestDto {

    @NotBlank(message = "Gem type is required")
    @JsonProperty("gem_type")
    private String gemType;  // sapphire, ruby, emerald, diamond

    @NotNull(message = "Carat weight is required")
    @Positive(message = "Carat weight must be positive")
    @Max(value = 100, message = "Carat weight must be less than 100")
    @JsonProperty("carat_weight")
    private Double caratWeight;

    @NotBlank(message = "Gem color is required")
    @JsonProperty("gem_color")
    private String gemColor;  // blue, red, green, pink, etc.

    @NotBlank(message = "Color quality is required")
    @JsonProperty("color_quality")
    private String colorQuality;  // vivid, royal, cornflower, normal, light

    @NotNull(message = "Clarity score is required")
    @Min(value = 1, message = "Clarity score must be at least 1")
    @Max(value = 5, message = "Clarity score must be at most 5")
    @JsonProperty("clarity_score")
    private Integer clarityScore;

    @NotNull(message = "Cut grade score is required")
    @Min(value = 1, message = "Cut grade score must be at least 1")
    @Max(value = 5, message = "Cut grade score must be at most 5")
    @JsonProperty("cut_grade_score")
    private Integer cutGradeScore;

    @NotBlank(message = "Shape is required")
    private String shape;  // oval, cushion, round, emerald, pear, etc.

    @NotBlank(message = "Origin is required")
    private String origin;  // sri lanka, myanmar, colombia, etc.

    @NotBlank(message = "Treatment is required")
    private String treatment;  // Heated, Unheated, Oiled

    // Optional dimensions
    private Double x;
    private Double y;
    private Double z;
}