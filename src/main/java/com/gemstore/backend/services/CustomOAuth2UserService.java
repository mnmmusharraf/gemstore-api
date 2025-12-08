package com.gemstore.backend.services;

import com.gemstore.backend.entities.User;
import com.gemstore.backend.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;

    private static final Logger logger = LoggerFactory.getLogger(CustomOAuth2UserService.class);

    @Override
    public OAuth2User loadUser(OAuth2UserRequest req) throws OAuth2AuthenticationException {
        String registrationId = req.getClientRegistration().getRegistrationId(); // google/github
        logger.info("[OAuth2UserService] OAuth2 login triggered for client: {}", registrationId);

        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User rawUser = delegate.loadUser(req);

        Map<String, Object> attrs = rawUser.getAttributes();

        String email = null;
        String displayName = null;
        String providerId = null;
        String avatar = null;

        if ("google".equals(registrationId)) {
            providerId = (String) attrs.get("sub");
            email = (String) attrs.get("email");
            displayName = (String) attrs.get("name");
            avatar = (String) attrs.get("picture");
        } else if ("github".equals(registrationId)) {
            providerId = String.valueOf(attrs.get("id"));
            email = (String) attrs.get("email"); // may be null
            displayName = (String) attrs.getOrDefault("name", attrs.get("login"));
            avatar = (String) attrs.get("avatar_url");
        } else {
            throw new OAuth2AuthenticationException(new OAuth2Error("unsupported_provider"), "Unsupported provider");
        }

        String provider = registrationId.toUpperCase();
        logger.info("[OAuth2UserService] provider={}, providerId={}, email={}", provider, providerId, email);

        User user = null;

        if (email != null) {
            user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        }
        if (user == null) {
            user = userRepository.findByProviderAndProviderId(provider, providerId).orElse(null);
        }

        if (user == null) {
            logger.info("[OAuth2UserService] No existing user, creating new one");
            user = new User();
            user.setProvider(provider);
            user.setProviderId(providerId);
            user.setEmail(email);
            user.setEmailVerified(email != null);
            user.setDisplayName(displayName);

            String baseUsername = generateUsernameCandidate(displayName, email, providerId);
            String uniqueUsername = ensureUniqueUsername(baseUsername);
            user.setUsername(uniqueUsername);

            user.setAvatarUrl(avatar);
            userRepository.save(user);
        } else {
            logger.info("[OAuth2UserService] Found existing user id={}", user.getId());
            boolean changed = false;
            if (avatar != null && (user.getAvatarUrl() == null || !user.getAvatarUrl().equals(avatar))) {
                user.setAvatarUrl(avatar);
                changed = true;
            }
            if (displayName != null && !displayName.equals(user.getDisplayName())) {
                user.setDisplayName(displayName);
                changed = true;
            }
            if (email != null && user.getEmail() == null) {
                user.setEmail(email);
                user.setEmailVerified(true);
                changed = true;
            }
            if (changed) {
                userRepository.save(user);
            }
        }

        return rawUser;
    }

    private String generateUsernameCandidate(String displayName, String email, String providerId) {
        if (displayName != null) return displayName.replaceAll("\\s+", "").toLowerCase();
        if (email != null) return email.split("@")[0];
        return "user" + providerId;
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