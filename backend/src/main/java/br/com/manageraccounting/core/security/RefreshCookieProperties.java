package br.com.manageraccounting.core.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Atributos do cookie HttpOnly que carrega o refresh token.
 */
@ConfigurationProperties(prefix = "app.security.refresh-cookie")
public record RefreshCookieProperties(
        @DefaultValue("true") boolean secure,
        @DefaultValue("Strict") String sameSite) {
}
