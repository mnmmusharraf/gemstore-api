package com.gemstore.backend.dtos;

import jakarta.validation.constraints.*;

import lombok.Data;

@Data
public class RegisterUserRequest {

    @NotBlank
    @Size(max = 150)
    private String displayName; // optional if you allow null; add @NotBlank only if required

    @NotBlank
    @Email
    @Size(max = 200)
    private String email;

    @NotBlank
    @Size(min = 8, max = 128, message = "Password must be 8–128 characters")
    // Optional: add a custom constraint for complexity
    private String password;

    @NotBlank
    @Size(min = 3, max = 40)
    private String username;

    // Optional future fields: locale, timezone
}
