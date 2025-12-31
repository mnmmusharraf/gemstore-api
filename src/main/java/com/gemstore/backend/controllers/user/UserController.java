package com.gemstore.backend.controllers.user;

import com.gemstore.backend.dtos.user.UpdateProfileRequest;
import com.gemstore.backend.dtos.user.UserResponse;
import com.gemstore.backend.mappers.user.UserMapper;
import com.gemstore.backend.security.CustomUserDetails;
import com.gemstore.backend.services.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    // ==================== GENERAL ====================

    @GetMapping
    public ResponseEntity<List<UserResponse>> listAll() {
        return ResponseEntity.ok(
                userService.findAll()
                        .stream()
                        .map(userMapper::toUserResponse)
                        .toList()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(
                userMapper.toUserResponse(userService.getById(id))
        );
    }

    // ==================== /me ====================

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        return ResponseEntity.ok(
                userMapper.toUserResponse(userService.getById(principal.getId()))
        );
    }

    @PatchMapping("/me")
    public ResponseEntity<UserResponse> patchProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        return ResponseEntity.ok(
                userMapper.toUserResponse(
                        userService.updateProfile(principal.getId(), request)
                )
        );
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        return ResponseEntity.ok(
                userMapper.toUserResponse(
                        userService.updateProfile(principal.getId(), request)
                )
        );
    }

    @PostMapping("/me/avatar")
    public ResponseEntity<Map<String, String>> uploadAvatar(
            @RequestParam("avatar") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        String avatarUrl = userService.uploadAvatar(principal.getId(), file);
        return ResponseEntity.ok(Map.of("avatarUrl", avatarUrl));
    }

    @DeleteMapping("/me/avatar")
    public ResponseEntity<Void> deleteAvatar(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        userService.deleteAvatar(principal.getId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteSelf(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        userService.softDelete(principal.getId());
        return ResponseEntity.noContent().build();
    }

    // ==================== ADMIN ====================

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        userService.deleteHard(id);
        return ResponseEntity.noContent().build();
    }
}
