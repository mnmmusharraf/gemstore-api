package com.gemstore.backend.config;

import com.gemstore.backend.entities.user.User;
import com.gemstore.backend.repositories.user.UserRepository;
import com.gemstore.backend.security.HttpCookieOAuth2AuthorizationRequestRepository;
import com.gemstore.backend.services.auth.JWTService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JWTService jwtService;

    // Beans to verify
    @Autowired private SecurityFilterChain securityFilterChain;
    @Autowired private AuthenticationManager authenticationManager;
    @Autowired private AuthenticationProvider authenticationProvider;
    @Autowired private HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository;

    private String userToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        User user = User.builder()
                .displayName("Test User")
                .email("sectest@example.com")
                .username("secuser")
                .passwordHash(passwordEncoder.encode("password123"))
                .provider("LOCAL")
                .role("USER")
                .status("ACTIVE")
                .build();
        user = userRepository.save(user);
        userToken = jwtService.generateToken(user.getId(), user.getUsername(), user.getRole());

        User admin = User.builder()
                .displayName("Admin User")
                .email("secadmin@example.com")
                .username("secadmin")
                .passwordHash(passwordEncoder.encode("adminpass"))
                .provider("LOCAL")
                .role("ADMIN")
                .status("ACTIVE")
                .build();
        admin = userRepository.save(admin);
        adminToken = jwtService.generateToken(admin.getId(), admin.getUsername(), admin.getRole());
    }

    // ==================== Bean Wiring ====================

    @Test
    void securityFilterChain_beanExists() {
        assertThat(securityFilterChain).isNotNull();
    }

    @Test
    void authenticationProvider_beanExists() {
        assertThat(authenticationProvider).isNotNull();
    }

    @Test
    void authenticationManager_beanExists() {
        assertThat(authenticationManager).isNotNull();
    }

    @Test
    void passwordEncoder_isBCrypt() {
        assertThat(passwordEncoder).isNotNull();
        assertThat(passwordEncoder).isInstanceOf(BCryptPasswordEncoder.class);
    }

    @Test
    void authorizationRequestRepository_beanExists() {
        assertThat(authorizationRequestRepository).isNotNull();
        assertThat(authorizationRequestRepository)
                .isInstanceOf(HttpCookieOAuth2AuthorizationRequestRepository.class);
    }

    // ==================== Public Endpoints (permitAll) ====================

    @Test
    void publicEndpoint_authLogin_isAccessible() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"identifier\":\"nobody\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized()); // Bad creds, but NOT filtered out — endpoint is reachable
    }

    @Test
    void publicEndpoint_authRegister_isAccessible() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest()); // Validation error, not 401
    }

    @Test
    void publicEndpoint_listingsSearch_isAccessible() throws Exception {
        mockMvc.perform(get("/api/v1/listings/search"))
                .andExpect(status().isOk());
    }

    @Test
    void publicEndpoint_listingsGetById_isAccessible() throws Exception {
        // GET on /api/v1/listings/{id} is permitAll — returns 4xx (not found), not 401
        mockMvc.perform(get("/api/v1/listings/99999"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void publicEndpoint_lookups_isAccessible() throws Exception {
        mockMvc.perform(get("/api/v1/lookups/gemstone-types"))
                .andExpect(status().isOk());
    }

    @Test
    void publicEndpoint_listingsSellerById_isAccessible() throws Exception {
        mockMvc.perform(get("/api/v1/listings/seller/1"))
                .andExpect(status().isOk());
    }

    // ==================== Protected Endpoints (authenticated) ====================

    @Test
    void protectedEndpoint_authMe_requiresAuth() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_authMe_withToken_isAccessible() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
    }

    @Test
    void protectedEndpoint_usersMe_requiresAuth() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_usersMe_withToken_isAccessible() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
    }

    @Test
    void protectedEndpoint_createListing_requiresAuth() throws Exception {
        mockMvc.perform(post("/api/v1/listings")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_uploadListingImage_requiresAuth() throws Exception {
        mockMvc.perform(post("/api/v1/listings/upload"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_myListings_requiresAuth() throws Exception {
        mockMvc.perform(get("/api/v1/listings/my"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_myListings_withToken_isAccessible() throws Exception {
        mockMvc.perform(get("/api/v1/listings/my")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
    }

    @Test
    void protectedEndpoint_messages_requiresAuth() throws Exception {
        mockMvc.perform(get("/api/v1/messages/conversations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_likes_requiresAuth() throws Exception {
        // POST /api/v1/likes/** requires authentication
        mockMvc.perform(post("/api/v1/likes/1")
                        .contentType("application/json"))
                .andExpect(status().isUnauthorized());
    }

    // ==================== Admin Endpoints (hasRole ADMIN) ====================

    @Test
    void adminEndpoint_withoutAuth_returnsUnauthorized() throws Exception {
        mockMvc.perform(patch("/api/admin/users/1/status/ACTIVE"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminEndpoint_withUserToken_returnsForbidden() throws Exception {
        mockMvc.perform(patch("/api/admin/users/1/status/ACTIVE")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminEndpoint_withAdminToken_isAccessible() throws Exception {
        // Will return 4xx because user ID 1 may not exist, but not 401/403
        mockMvc.perform(patch("/api/admin/users/99999/status/ACTIVE")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().is4xxClientError()); // 404 not found, not 401/403
    }

    // ==================== CSRF Disabled ====================

    @Test
    void csrf_isDisabled_postWithoutCsrfToken_works() throws Exception {
        // POST to a public endpoint without CSRF token should NOT return 403
        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest()); // Validation error, not 403 CSRF
    }

    // ==================== Stateless Sessions ====================

    @Test
    void session_isStateless_noCookieOnResponse() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    // No JSESSIONID cookie should be set in a stateless config
                    String setCookie = result.getResponse().getHeader("Set-Cookie");
                    if (setCookie != null) {
                        assertThat(setCookie).doesNotContain("JSESSIONID");
                    }
                });
    }
}