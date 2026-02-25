package com.gemstore.backend.config;

import com.gemstore. backend.services.auth.CustomOAuth2UserService;
import com.gemstore.backend.security. HttpCookieOAuth2AuthorizationRequestRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security. authentication.AuthenticationManager;
import org.springframework.security.authentication. AuthenticationProvider;
import org. springframework.security.authentication.dao. DaoAuthenticationProvider;
import org.springframework.security.config. Customizer;
import org. springframework.security.config.annotation. authentication.configuration.AuthenticationConfiguration;
import org.springframework.security. config.annotation.web.builders. HttpSecurity;
import org.springframework.security.config.annotation. web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework. security.core.userdetails. UserDetailsService;
import org. springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto. password.PasswordEncoder;
import org.springframework.security.web. SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsService userDetailsService;
    private final JwtFilter jwtFilter;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final OAuth2FailureHandler oAuth2FailureHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())

                .sessionManagement(sm ->
                        sm.sessionCreationPolicy(SessionCreationPolicy. STATELESS)
                )

                .exceptionHandling(ex ->
                        ex.authenticationEntryPoint((req, res, e) ->
                                res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized")
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        // WebSocket
                        .requestMatchers("/ws/**", "/ws/info/**").permitAll()

                        //  Static resources - uploaded images (PUBLIC)
                        .requestMatchers("/uploads/**").permitAll()

                        // Auth endpoints
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/register",
                                "/oauth2/**",
                                "/login/oauth2/**"
                        ).permitAll()

                        //  GEM PRICE PUBLIC ENDPOINTS
                        .requestMatchers("/api/v1/gems/price/options").permitAll()
                        .requestMatchers("/api/v1/gems/price/health").permitAll()

                        //  GEM PRICE PROTECTED ENDPOINTS
                        .requestMatchers("/api/v1/gems/price/predict").authenticated()
                        .requestMatchers("/api/v1/gems/price/**").authenticated()

                        // Public APIs
                        .requestMatchers("/api/v1/lookups/**").permitAll()
                        .requestMatchers("/api/v1/listings/search").permitAll()
                        .requestMatchers("/api/v1/listings/seller/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/listings/**").permitAll()
                        .requestMatchers("/api/test").permitAll()

                        // Upload requires auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/listings/upload").authenticated()

                        // Likes
                        .requestMatchers(HttpMethod.GET, "/api/v1/likes/*/count").permitAll()
                        .requestMatchers("/api/v1/likes/**").authenticated()

                        // Everything else
                        .anyRequest().authenticated()
                )

                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(authz ->
                                authz.authorizationRequestRepository(authorizationRequestRepository())
                        )
                        .userInfoEndpoint(ui ->
                                ui.userService(customOAuth2UserService)
                        )
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler(oAuth2FailureHandler)
                )

                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setPasswordEncoder(passwordEncoder());
        provider.setUserDetailsService(userDetailsService);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository() {
        return new HttpCookieOAuth2AuthorizationRequestRepository();
    }

//    @Bean
//    public CorsConfigurationSource corsConfigurationSource() {
//        CorsConfiguration config = new CorsConfiguration();
//
//        config.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:3000",
//                "*"));
//        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
//        config.setAllowedHeaders(List.of("*"));
//        config.setAllowCredentials(true);
//
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/**", config);
//
//        return source;
//    }
}