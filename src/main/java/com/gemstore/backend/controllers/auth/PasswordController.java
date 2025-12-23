package com.gemstore.backend.controllers.auth;

import com.gemstore.backend.dtos.auth.ChangePasswordRequest;
import com.gemstore.backend.security.CustomUserDetails;
import com.gemstore.backend.services.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Dedicated password operations.
 */
@RestController
@RequestMapping("/api/auth/password")
@RequiredArgsConstructor
public class PasswordController {

    private final UserService userService;

    /**
     * Change current user's password.
     */
    @PostMapping("/change")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        userService.changePassword(principal.getId(), request);
        return ResponseEntity.noContent().build();
    }

    // Future:
    //  POST /reset/request  (send email)
    //  POST /reset/confirm  (token + new password)
}
