////package com.gemstore.backend.services.auth;
////
////
////import io.jsonwebtoken.*;
////import io.jsonwebtoken.io.Decoders;
////import io.jsonwebtoken.security.Keys;
////import org.springframework.beans.factory.annotation.Value;
////import org.springframework.stereotype.Service;
////
////import javax.crypto.SecretKey;
////import java.time.Duration;
////import java.util.Date;
////import java.util.Map;
////
////@Service
////public class JWTService {
////
////    @Value("${security.jwt.secret}")
////    private String secretKeyBase64;
////
////    @Value("${security.jwt.ttl-seconds:3600}") // 1 hour default
////    private long ttlSeconds;
////
////    private SecretKey getKey() {
////        byte[] keyBytes = Decoders.BASE64.decode(secretKeyBase64);
////        return Keys.hmacShaKeyFor(keyBytes);
////    }
////
////    public String generateToken(String subject, Map<String,Object> extraClaims) {
////        long nowMillis = System.currentTimeMillis();
////        long expMillis = nowMillis + Duration.ofSeconds(ttlSeconds).toMillis();
////        return Jwts.builder()
////                .claims(extraClaims)
////                .subject(subject)
////                .issuedAt(new Date(nowMillis))
////                .expiration(new Date(expMillis))
////                .signWith(getKey())
////                .compact();
////    }
////
////    public String generateToken(String subject) {
////        return generateToken(subject, Map.of());
////    }
////
////    public String extractUsername(String token) {
////        return parseAllClaims(token).getSubject();
////    }
////
////    public boolean isValid(String token, String expectedSubject) {
////        Claims claims = parseAllClaims(token);
////        return !claims.getExpiration().before(new Date()) &&
////                expectedSubject.equals(claims.getSubject());
////    }
////
////    private Claims parseAllClaims(String token) {
////        return Jwts.parser()
////                .verifyWith(getKey())
////                .build()
////                .parseSignedClaims(token)
////                .getPayload();
////    }
////}
//
//package com.gemstore.backend.services.auth;
//
//import io.jsonwebtoken.*;
//import io.jsonwebtoken.io.Decoders;
//import io.jsonwebtoken.security.Keys;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//
//import javax.crypto.SecretKey;
//import java.time.Duration;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.Map;
//
//@Slf4j
//@Service
//public class JWTService {
//
//    @Value("${security.jwt.secret}")
//    private String secretKeyBase64;
//
//    @Value("${security.jwt.ttl-seconds:3600}") // 1 hour default
//    private long ttlSeconds;
//
//    private SecretKey getKey() {
//        byte[] keyBytes = Decoders.BASE64.decode(secretKeyBase64);
//        return Keys.hmacShaKeyFor(keyBytes);
//    }
//
//    /**
//     * ✅ NEW METHOD: Generate token with user ID, username, and role.
//     * This is what your login endpoint should call!
//     */
//    public String generateToken(Long userId, String username, String role) {
//        Map<String, Object> claims = new HashMap<>();
//        claims.put("uid", userId);    // ✅ CRITICAL: Include user ID
//        claims.put("role", role);
//
//        log.info("[JWT Service] Generating token - userId: {}, username: {}, role: {}",
//                userId, username, role);
//
//        long nowMillis = System.currentTimeMillis();
//        long expMillis = nowMillis + Duration.ofSeconds(ttlSeconds).toMillis();
//
//        return Jwts.builder()
//                .claims(claims)
//                .subject(username)
//                .issuedAt(new Date(nowMillis))
//                .expiration(new Date(expMillis))
//                .signWith(getKey())
//                .compact();
//    }
//
//    /**
//     * Original method - now delegates to the new one with default role.
//     */
//    public String generateToken(String subject, Map<String, Object> extraClaims) {
//        long nowMillis = System.currentTimeMillis();
//        long expMillis = nowMillis + Duration.ofSeconds(ttlSeconds).toMillis();
//
//        return Jwts.builder()
//                .claims(extraClaims)
//                .subject(subject)
//                .issuedAt(new Date(nowMillis))
//                .expiration(new Date(expMillis))
//                .signWith(getKey())
//                .compact();
//    }
//
//    /**
//     * Original method - kept for backward compatibility.
//     */
//    public String generateToken(String subject) {
//        return generateToken(subject, Map.of());
//    }
//
//    /**
//     * ✅ NEW METHOD: Extract user ID from token.
//     */
//    public Long extractUserId(String token) {
//        Claims claims = parseAllClaims(token);
//        Object uid = claims.get("uid");
//
//        if (uid == null) {
//            log.error("[JWT Service] Token doesn't contain 'uid' claim!");
//            return null;
//        }
//
//        // Handle both Integer and Long
//        if (uid instanceof Integer) {
//            return ((Integer) uid).longValue();
//        } else if (uid instanceof Long) {
//            return (Long) uid;
//        }
//
//        log.error("[JWT Service] 'uid' claim is not a number: {}", uid.getClass().getName());
//        return null;
//    }
//
//    /**
//     * ✅ NEW METHOD: Extract role from token.
//     */
//    public String extractRole(String token) {
//        Claims claims = parseAllClaims(token);
//        return claims.get("role", String.class);
//    }
//
//    /**
//     * Original method - extract username.
//     */
//    public String extractUsername(String token) {
//        return parseAllClaims(token).getSubject();
//    }
//
//    /**
//     * Original method - validate token.
//     */
//    public boolean isValid(String token, String expectedSubject) {
//        try {
//            Claims claims = parseAllClaims(token);
//            return !claims.getExpiration().before(new Date()) &&
//                    expectedSubject.equals(claims.getSubject());
//        } catch (Exception e) {
//            log.error("[JWT Service] Token validation failed", e);
//            return false;
//        }
//    }
//
//    /**
//     * Original method - parse claims.
//     */
//    private Claims parseAllClaims(String token) {
//        return Jwts.parser()
//                .verifyWith(getKey())
//                .build()
//                .parseSignedClaims(token)
//                .getPayload();
//    }
//
//    /**
//     * ✅ NEW METHOD: Extract all claims publicly (needed by JwtFilter).
//     */
//    public Claims extractAllClaims(String token) {
//        return parseAllClaims(token);
//    }
//
//    /**
//     * ✅ NEW METHOD: Debug token contents (for testing).
//     */
//    public void debugToken(String token) {
//        try {
//            Claims claims = parseAllClaims(token);
//            log.info("[JWT Debug] Token claims: {}", claims);
//            log.info("[JWT Debug] Username: {}", claims.getSubject());
//            log.info("[JWT Debug] UID: {}", claims.get("uid"));
//            log.info("[JWT Debug] Role: {}", claims.get("role"));
//            log.info("[JWT Debug] Expiration: {}", claims.getExpiration());
//        } catch (Exception e) {
//            log.error("[JWT Debug] Failed to parse token", e);
//        }
//    }
//}
//


package com.gemstore.backend.services.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class JWTService {

    @Value("${security.jwt.secret}")
    private String secretKeyBase64;

    @Value("${security.jwt.ttl-seconds:3600}")
    private long ttlSeconds;

    /* ===================== Key ===================== */

    private SecretKey getKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKeyBase64);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /* ===================== TOKEN CREATION (ONLY WAY) ===================== */

    /**
     * ✅ SINGLE SOURCE OF TRUTH
     * Every token MUST contain uid + role
     */
    public String generateToken(Long userId, String username, String role) {

        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null when generating JWT");
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("uid", userId);
        claims.put("role", role);

        long now = System.currentTimeMillis();
        long exp = now + Duration.ofSeconds(ttlSeconds).toMillis();

        log.info("[JWT] Generating token | uid={}, username={}, role={}",
                userId, username, role);

        return Jwts.builder()
                .subject(username)           // sub
                .claims(claims)              // uid, role
                .issuedAt(new Date(now))
                .expiration(new Date(exp))
                .signWith(getKey())
                .compact();
    }

    /* ===================== EXTRACTION ===================== */

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public Long extractUserId(String token) {
        Object uid = extractAllClaims(token).get("uid");

        if (uid == null) {
            log.error("[JWT] Missing 'uid' claim");
            return null;
        }

        if (uid instanceof Integer i) return i.longValue();
        if (uid instanceof Long l) return l;

        throw new IllegalStateException("Invalid uid claim type: " + uid.getClass());
    }

    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    /* ===================== VALIDATION ===================== */

    public boolean isValid(String token, String expectedUsername) {
        try {
            Claims claims = extractAllClaims(token);
            return !claims.getExpiration().before(new Date())
                    && expectedUsername.equals(claims.getSubject());
        } catch (Exception e) {
            log.error("[JWT] Token validation failed", e);
            return false;
        }
    }

    /* ===================== INTERNAL ===================== */

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
