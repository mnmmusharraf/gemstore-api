package com.gemstore.backend.controllers.auth;


import com.gemstore.backend.dtos.auth.ChangePasswordRequest;
import com.gemstore.backend.security.UserPrincipal;
import com.gemstore.backend.services.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Dedicated password operations (optional; can also live in AuthController).
 */
@RestController
@RequestMapping("/api/auth/password")
@RequiredArgsConstructor
public class PasswordController {

    private final UserService userService;

    @PostMapping("/change")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                               Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        userService.changePassword(principal.getId(), request);
        return ResponseEntity.noContent().build();
    }

    // Future:
    //  POST /reset/request  (send email)
    //  POST /reset/confirm  (token + new password)
}
