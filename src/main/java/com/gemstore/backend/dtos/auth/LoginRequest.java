package com.gemstore.backend.dtos.auth;


import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank
    private String identifier; // Accepts username OR email

    @NotBlank
    private String password;
}