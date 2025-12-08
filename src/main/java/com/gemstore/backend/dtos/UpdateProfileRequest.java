package com.gemstore.backend.dtos;


import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateProfileRequest {

    @Size(max = 150)
    private String displayName;

    @Size(max = 100)
    private String firstName;

    @Size(max = 100)
    private String lastName;

    @Size(max = 500)
    private String avatarUrl;

    @Size(max = 64)
    private String timezone;

    @Size(max = 16)
    private String locale;
}
