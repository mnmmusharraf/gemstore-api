package com.gemstore.backend.config;

import com.gemstore. backend.services.auth.JWTService;
import com.gemstore.backend.services.auth. MyUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security. authentication.UsernamePasswordAuthenticationToken;
import org.springframework. security.core.context.SecurityContextHolder;
import org.springframework. security.web.authentication.WebAuthenticationDetailsSource;
import org. springframework.stereotype.Component;
import org.springframework.web.filter. OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JWTService jwtService;
    private final MyUserDetailsService userDetailsService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request. getRequestURI();
        String method = request.getMethod();

        // Auth endpoints
        if (path.startsWith("/api/auth/login") ||
                path.startsWith("/api/auth/register") ||
                path.startsWith("/oauth2") ||
                path.startsWith("/login/oauth2") ||
                path.startsWith("/uploads")) {
            return true;
        }

        // ✅ Public API endpoints (GET only)
        if ("GET".equalsIgnoreCase(method)) {
            if (path.startsWith("/api/v1/lookups") ||
                    path.equals("/api/v1/listings") ||
                    path.startsWith("/api/v1/listings/search") ||
                    path. startsWith("/api/v1/listings/seller/") ||
                    path.matches("/api/v1/listings/\\d+") ||
                    path.matches("/api/v1/listings/\\d+/detail") ||
                    path.equals("/api/test")) {
                return true;
            }
        }

        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        String username;
        try {
            username = jwtService.extractUsername(token);
        } catch (Exception e) {
            chain.doFilter(request, response);
            return;
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            var userDetails = userDetailsService. loadUserByUsername(username);
            if (jwtService.isValid(token, userDetails.getUsername())) {
                var authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                authToken. setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        chain.doFilter(request, response);
    }
}