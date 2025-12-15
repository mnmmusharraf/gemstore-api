package com.gemstore.backend.config;

import com.gemstore.backend.services. auth.CustomOAuth2UserService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory. annotation.Autowired;
import org.springframework.context.annotation. Bean;
import org.springframework. context.annotation.Configuration;
import org.springframework.security.authentication. AuthenticationManager;
import org. springframework.security.authentication.AuthenticationProvider;
import org.springframework. security.authentication.dao.DaoAuthenticationProvider;
import org. springframework.security.config.Customizer;
import org.springframework. security.config.annotation.authentication. configuration.AuthenticationConfiguration;
import org.springframework.security.config. annotation.web.builders.HttpSecurity;
import org.springframework. security.config.annotation.web. configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security. core.userdetails.UserDetailsService;
import org.springframework. security.crypto.bcrypt.BCryptPasswordEncoder;
import org. springframework.security.crypto.password. PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web. authentication.UsernamePasswordAuthenticationFilter;
import org.springframework. web.cors.CorsConfiguration;
import org.springframework.web. cors.CorsConfigurationSource;
import org.springframework.web. cors.UrlBasedCorsConfigurationSource;
import com. gemstore.backend.security.HttpCookieOAuth2AuthorizationRequestRepository;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private JwtFilter jwtFilter;

    @Autowired
    CustomOAuth2UserService customOAuth2UserService;

    @Bean
    public HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository() {
        return new HttpCookieOAuth2AuthorizationRequestRepository();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   OAuth2SuccessHandler oAuth2SuccessHandler,
                                                   OAuth2FailureHandler oAuth2FailureHandler) throws Exception {

        http
                . cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth
                        // Auth endpoints
                        .requestMatchers("/api/auth/register", "/api/auth/login", "/login/oauth2/**", "/oauth2/**").permitAll()

                        // ✅ ADD THESE LINES - Public API endpoints
                        .requestMatchers("/api/v1/lookups/**").permitAll()
                        . requestMatchers("/api/v1/listings").permitAll()
                        .requestMatchers("/api/v1/listings/search").permitAll()
                        . requestMatchers("/api/v1/listings/seller/**").permitAll()
                        .requestMatchers("/api/v1/listings/{id}").permitAll()
                        .requestMatchers("/api/v1/listings/{id}/detail").permitAll()
                        . requestMatchers("/api/test").permitAll()

                        // Static resources
                        .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                        .requestMatchers("/uploads/**").permitAll()

                        // All other requests require authentication
                        .anyRequest().authenticated()
                )

                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"))
                )

                .oauth2Login(oauth2 -> oauth2
                        . authorizationEndpoint(authorization -> authorization
                                .authorizationRequestRepository(authorizationRequestRepository())
                        )
                        .userInfoEndpoint(ui -> ui.userService(customOAuth2UserService))
                        . successHandler(oAuth2SuccessHandler)
                        .failureHandler(oAuth2FailureHandler)
                )

                .httpBasic(Customizer.withDefaults())

                .sessionManagement(sm -> sm
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .maximumSessions(1)
                )

                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setPasswordEncoder(passwordEncoder());
        provider.setUserDetailsService(userDetailsService);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManagerBean(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(java.util.List.of("http://localhost:5173"));
        config.setAllowedMethods(java.util.List. of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(java. util.List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}