package com.gemstore.backend.services.auth;


import com.gemstore.backend.dtos.auth.AuthResponse;
import com.gemstore.backend.dtos.auth.LoginRequest;
import com.gemstore.backend.dtos.auth.RegisterUserRequest;
import com.gemstore.backend.entities.user.User;
import com.gemstore.backend.mappers.user.UserMapper;
import com.gemstore.backend.repositories.user.UserRepository;
import com.gemstore.backend.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JWTService jwtService;
    private final EmailVerificationOtpService otpService;

    public AuthResponse register(RegisterUserRequest req) {

        String emailNorm = req.getEmail().trim().toLowerCase();
        String usernameNorm = req.getUsername().trim();
        String displayNameNorm = req.getDisplayName().trim();

        if (userRepository.existsByEmailIgnoreCase(emailNorm)) {
            throw new IllegalStateException("Email already used");
        }

        if (userRepository.existsByUsernameIgnoreCase(usernameNorm)) {
            throw new IllegalStateException("Username already taken");
        }

        User user = userMapper.toEntity(req);

        user.setEmail(emailNorm);
        user.setUsername(usernameNorm);
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setDisplayName(displayNameNorm);

        if (user.getRole() == null) {
            user.setRole("USER");
        }

        // Save user first
        user = userRepository.save(user);

        // Generate and send OTP email
        otpService.generateAndSendOtp(user);

        // Generate JWT
        String token = jwtService.generateToken(
                user.getId(),
                user.getUsername(),
                user.getRole()
        );

        log.info("[AuthService] Registration token generated for userId={}",
                user.getId());

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .user(userMapper.toUserResponse(user))
                .newUser(true)
                .build();
    }


    public AuthResponse login(LoginRequest request) {

        // Authenticate (this triggers UserDetailsService)
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getIdentifier(),
                        request.getPassword()
                )
        );

        // Get authenticated user
        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        //  Extract role (remove ROLE_ prefix)
        String role = userDetails.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("USER");

        //  Generate JWT with userId + username + role
        String token = jwtService.generateToken(
                userDetails.getId(),       // userId
                userDetails.getUsername(), // subject
                role                       // role
        );

        // Log
        log.info("[AuthService] Token generated for userId={}, username={}",
                userDetails.getId(),
                userDetails.getUsername());

        // Build response
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .user(userMapper.toUserResponse(
                        userRepository.findActiveById(userDetails.getId()).orElseThrow()
                ))
                .newUser(false)
                .build();
    }

}