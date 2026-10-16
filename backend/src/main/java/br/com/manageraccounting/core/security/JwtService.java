package br.com.manageraccounting.core.security;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Gera e valida o access token (JWT assinado com HS256).
 * O token carrega apenas {@code userId} e {@code tenantId} (além de iat/exp).
 */
@Service
public class JwtService {

    static final String CLAIM_USER_ID = "userId";
    static final String CLAIM_TENANT_ID = "tenantId";

    private final SecretKey key;
    private final JwtProperties properties;
    private final Clock clock;

    public JwtService(JwtProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public AccessToken generateAccessToken(Long userId, Long tenantId) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(properties.accessTokenTtl());

        String token = Jwts.builder()
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_TENANT_ID, tenantId)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        return new AccessToken(token, properties.accessTokenTtl().toSeconds());
    }

    /**
     * Valida assinatura e validade do token. Retorna vazio se for inválido, expirado ou malformado.
     */
    public Optional<TokenClaims> parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Object userId = claims.get(CLAIM_USER_ID);
            Object tenantId = claims.get(CLAIM_TENANT_ID);
            if (!(userId instanceof Number) || !(tenantId instanceof Number)) {
                return Optional.empty();
            }
            return Optional.of(new TokenClaims(((Number) userId).longValue(), ((Number) tenantId).longValue()));
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public record AccessToken(String value, long expiresInSeconds) {
    }

    public record TokenClaims(Long userId, Long tenantId) {
    }
}
