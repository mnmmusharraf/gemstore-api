package com.gemstore.backend.dtos.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyEmailOtpRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String otp;
}