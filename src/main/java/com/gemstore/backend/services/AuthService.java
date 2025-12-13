package com.gemstore.backend.services;


import com.gemstore.backend.dtos.AuthResponse;
import com.gemstore.backend.dtos.LoginRequest;
import com.gemstore.backend.dtos.RegisterUserRequest;
import com.gemstore.backend.entities.user.User;
import com.gemstore.backend.mappers.UserMapper;
import com.gemstore.backend.repositories.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JWTService jwtService;

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
        try {
            userRepository.save(user);
        } catch (Exception e) {
            // handle database error, e.g., log and throw a custom exception
            throw new RuntimeException("Failed to register user", e);
        }

        String token = jwtService.generateToken(user.getUsername());
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .user(userMapper.toUserResponse(user))
                .newUser(true)
                .build();
    }

    public AuthResponse login(LoginRequest req) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getIdentifier(), req.getPassword()));

        // Resolve user so token subject is consistent (username)
        User user = userRepository.findByEmailIgnoreCase(req.getIdentifier())
                .or(() -> userRepository.findByUsernameIgnoreCase(req.getIdentifier()))
                .orElseThrow(() -> new IllegalStateException("Invalid credentials")); // Shouldn't happen if auth passed

        String token = jwtService.generateToken(user.getUsername());
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .user(userMapper.toUserResponse(user))
                .newUser(false)
                .build();
    }
}