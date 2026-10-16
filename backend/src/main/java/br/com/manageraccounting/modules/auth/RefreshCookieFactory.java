package br.com.manageraccounting.modules.auth;

import java.time.Duration;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import br.com.manageraccounting.core.security.RefreshCookieProperties;

/**
 * Monta o cookie do refresh token: HttpOnly (o JavaScript não lê), restrito a /api/auth e com SameSite.
 */
@Component
public class RefreshCookieFactory {

    public static final String COOKIE_NAME = "refresh_token";
    private static final String COOKIE_PATH = "/api/auth";

    private final RefreshCookieProperties properties;

    public RefreshCookieFactory(RefreshCookieProperties properties) {
        this.properties = properties;
    }

    public ResponseCookie criar(String refreshToken, Duration ttl) {
        return base(refreshToken).maxAge(ttl).build();
    }

    public ResponseCookie expirar() {
        return base("").maxAge(Duration.ZERO).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(properties.secure())
                .sameSite(properties.sameSite())
                .path(COOKIE_PATH);
    }
}
