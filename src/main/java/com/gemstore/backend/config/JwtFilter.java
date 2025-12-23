package com.gemstore.backend.config;

import com.gemstore.backend.security.CustomUserDetails;
import com.gemstore.backend.services.auth.JWTService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JWTService jwtService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        // Skip filter for auth endpoints
        return path.startsWith("/api/auth/login") ||
                path.startsWith("/api/auth/register") ||
                path.startsWith("/oauth2") ||
                path.startsWith("/login/oauth2");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        log.debug("[JWT Filter] Processing: {} {}", request.getMethod(), requestURI);

        // Extract token from Authorization header
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("[JWT Filter] No Bearer token found");
            chain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        log.debug("[JWT Filter] Token found: {}...", token.substring(0, Math.min(20, token.length())));

        try {
            // Extract username from token
            String username = jwtService.extractUsername(token);

            if (username == null) {
                log.warn("[JWT Filter] Username is null in token");
                chain.doFilter(request, response);
                return;
            }

            log.debug("[JWT Filter] Extracted username: {}", username);

            // Only set authentication if not already authenticated
            if (SecurityContextHolder.getContext().getAuthentication() == null) {

                // Validate token
                if (!jwtService.isValid(token, username)) {
                    log.warn("[JWT Filter] Token validation failed for user: {}", username);
                    chain.doFilter(request, response);
                    return;
                }

                log.debug("[JWT Filter] Token is valid");

                // Extract user ID and role from JWT claims
                Long userId = jwtService.extractUserId(token);
                String role = jwtService.extractRole(token);

                if (userId == null) {
                    log.error("[JWT Filter] USER ID IS NULL! Token doesn't contain 'uid' claim");
                    log.error("[JWT Filter] This will cause 'user is NULL' in controllers");
                    log.error("[JWT Filter] Fix: Add .claim(\"uid\", userId) when generating token");
                    chain.doFilter(request, response);
                    return;
                }

                log.info("[JWT Filter] Creating CustomUserDetails - userId: {}, username: {}, role: {}",
                        userId, username, role);

                // Create CustomUserDetails directly from JWT claims
                CustomUserDetails userDetails = new CustomUserDetails(
                        userId,
                        username,
                        role != null ? role : "USER"
                );

                // Create authentication token
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Set authentication in SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authToken);

                log.info("[JWT Filter] Authentication set successfully! User ID: {}", userId);
            }
        } catch (Exception e) {
            log.error("[JWT Filter] Error during JWT authentication", e);
        }

        chain.doFilter(request, response);
    }
}