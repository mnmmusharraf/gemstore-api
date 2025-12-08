package com.gemstore.backend.config;

import com.gemstore.backend.entities.User;
import com.gemstore.backend.repositories.UserRepository;
import com.gemstore.backend.services.JWTService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.*;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JWTService jwtService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Logger logger = LoggerFactory.getLogger(OAuth2SuccessHandler.class);

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        logger.info("[OAuth2] login success for user: {}", authentication.getName());

        try {
            OAuth2User principal = (OAuth2User) authentication.getPrincipal();
            Map<String, Object> attrs = principal.getAttributes();

            // Detect provider + attributes in the same way as CustomOAuth2UserService
            String provider = "GOOGLE";
            String registrationIdGuess = "google";

            String email = null;
            String displayName = null;
            String providerId = null;
            String avatar = null;

            if (attrs.containsKey("sub")) { // Google
                providerId = (String) attrs.get("sub");
                email = (String) attrs.get("email");
                displayName = (String) attrs.get("name");
                avatar = (String) attrs.get("picture");
                provider = "GOOGLE";
                registrationIdGuess = "google";
            } else if (attrs.containsKey("id") && attrs.containsKey("login")) { // GitHub
                providerId = String.valueOf(attrs.get("id"));
                email = (String) attrs.get("email"); // may be null
                displayName = (String) attrs.getOrDefault("name", attrs.get("login"));
                avatar = (String) attrs.get("avatar_url");
                provider = "GITHUB";
                registrationIdGuess = "github";
            }

            logger.info("[OAuth2] Attributes: provider={}, providerId={}, email={}",
                    provider, providerId, email);

            // 1) Try to find existing user (email first, then provider+providerId)
            User user = null;

            if (email != null) {
                user = userRepository.findByEmailIgnoreCase(email).orElse(null);
            }
            if (user == null && providerId != null) {
                user = userRepository.findByProviderAndProviderId(provider, providerId).orElse(null);
            }

            // 2) If still not found, create a new user here as a fallback
            if (user == null) {
                logger.warn("[OAuth2] No linked user found, creating a new one (fallback).");
                user = new User();
                user.setProvider(provider);
                user.setProviderId(providerId);
                user.setEmail(email);
                user.setEmailVerified(email != null);
                user.setDisplayName(displayName);

                // Generate a simple username
                String baseUsername;
                if (displayName != null && !displayName.isBlank()) {
                    baseUsername = displayName.replaceAll("\\s+", "").toLowerCase();
                } else if (email != null && !email.isBlank()) {
                    baseUsername = email.split("@")[0];
                } else {
                    baseUsername = registrationIdGuess + "_" + (providerId != null ? providerId : "user");
                }
                String username = ensureUniqueUsername(baseUsername);
                user.setUsername(username);

                user.setAvatarUrl(avatar);
                if (user.getRole() == null) user.setRole("USER");
                if (user.getStatus() == null) user.setStatus("ACTIVE");

                user = userRepository.save(user);
                logger.info("[OAuth2] Created fallback user id={}, username={}", user.getId(), user.getUsername());
            }

            // 3) Ensure username exists
            String username = user.getUsername();
            if (username == null || username.isBlank()) {
                username = provider.toLowerCase() + "_" + user.getId();
                username = ensureUniqueUsername(username);
                user.setUsername(username);
                userRepository.save(user);
            }

            // 4) Generate JWT and redirect to frontend
            String token = jwtService.generateToken(username, Map.of(
                    "uid", user.getId(),
                    "role", user.getRole()
            ));

            String encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8);
            String redirectUrl = "http://localhost:5173/oauth2/success?token=" + encodedToken;

            logger.info("[OAuth2] Redirecting to frontend: {}", redirectUrl);
            response.sendRedirect(redirectUrl);

        } catch (Exception e) {
            logger.error("[OAuth2] Exception in success handler", e);
            redirectWithError(response, "OAuth2 Success Handler Error");
        }
    }

    private void redirectWithError(HttpServletResponse response, String message) throws IOException {
        String encodedError = URLEncoder.encode(message, StandardCharsets.UTF_8);
        String redirectUrl = "http://localhost:5173/oauth2/success?error=" + encodedError;
        response.sendRedirect(redirectUrl);
    }

    private String ensureUniqueUsername(String base) {
        if (base == null || base.isBlank()) base = "user";
        String candidate = base.replaceAll("\\W+", "").toLowerCase();
        if (candidate.isBlank()) candidate = "user";
        String original = candidate;
        int suffix = 1;
        while (userRepository.findByUsernameIgnoreCase(candidate).isPresent()) {
            candidate = original + suffix++;
        }
        return candidate;
    }
}