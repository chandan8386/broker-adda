package com.trishakti.crm.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;
    private final long refreshExpirationMs;
    private final String issuer;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs,
            @Value("${app.jwt.refresh-expiration-ms}") long refreshExpirationMs,
            @Value("${app.jwt.issuer}") String issuer) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
        this.issuer = issuer;
    }

    public String generateAccessToken(UserPrincipal user) {
        Date now = new Date();
        List<String> roles = user.getAuthorities().stream().map(Object::toString).toList();
        return Jwts.builder()
                .subject(user.getUsername())
                .issuer(issuer)
                .claim("uid", user.getId())
                .claim("email", user.getEmail())
                .claim("name", user.getFullName())
                .claim("roles", roles)
                .claim("type", "access")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(UserPrincipal user) {
        Date now = new Date();
        return Jwts.builder()
                .subject(user.getUsername())
                .issuer(issuer)
                .claim("uid", user.getId())
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + refreshExpirationMs))
                .signWith(key)
                .compact();
    }

    public String extractUsername(String token) {
        return parse(token, Claims::getSubject);
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = allClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception ex) {
            return false;
        }
    }

    public boolean isRefreshToken(String token) {
        try {
            return "refresh".equals(allClaims(token).get("type", String.class));
        } catch (Exception ex) {
            return false;
        }
    }

    public Map<String, Object> claims(String token) {
        return allClaims(token);
    }

    public long getExpirationMs() { return expirationMs; }
    public long getRefreshExpirationMs() { return refreshExpirationMs; }

    private <T> T parse(String token, Function<Claims, T> resolver) {
        return resolver.apply(allClaims(token));
    }

    private Claims allClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
