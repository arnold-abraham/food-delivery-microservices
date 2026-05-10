package com.example.userservice.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@Service
public class JwtService {

    private final Key signingKey;
    private final long ttlSeconds;

    public JwtService(
            @Value("${security.jwt.secret}") String base64Secret,
            @Value("${security.jwt.ttl-seconds:3600}") long ttlSeconds
    ) {
        byte[] keyBytes = Decoders.BASE64.decode(base64Secret);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET must decode to at least 256 bits (32 bytes). " +
                    "Generate one with: openssl rand -base64 48");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.ttlSeconds = ttlSeconds;
    }

    public String issueToken(Long userId, String email, List<String> roles) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(ttlSeconds);

        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("email", email)
                .claim("roles", roles)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }
}
