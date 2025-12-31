package com.gemstore.backend.dtos.auth;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RequestPasswordResetRequest {
    @NotBlank
    @Email
    private String email;
}