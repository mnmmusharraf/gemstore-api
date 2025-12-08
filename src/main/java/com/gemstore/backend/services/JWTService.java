package com.gemstore.backend.services;


import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Date;
import java.util.Map;

@Service
public class JWTService {

    @Value("${security.jwt.secret}")
    private String secretKeyBase64;

    @Value("${security.jwt.ttl-seconds:3600}") // 1 hour default
    private long ttlSeconds;

    private SecretKey getKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKeyBase64);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String subject, Map<String,Object> extraClaims) {
        long nowMillis = System.currentTimeMillis();
        long expMillis = nowMillis + Duration.ofSeconds(ttlSeconds).toMillis();
        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuedAt(new Date(nowMillis))
                .expiration(new Date(expMillis))
                .signWith(getKey())
                .compact();
    }

    public String generateToken(String subject) {
        return generateToken(subject, Map.of());
    }

    public String extractUsername(String token) {
        return parseAllClaims(token).getSubject();
    }

    public boolean isValid(String token, String expectedSubject) {
        Claims claims = parseAllClaims(token);
        return !claims.getExpiration().before(new Date()) &&
                expectedSubject.equals(claims.getSubject());
    }

    private Claims parseAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
