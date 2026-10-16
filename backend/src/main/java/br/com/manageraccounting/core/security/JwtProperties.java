package br.com.manageraccounting.core.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Configuração dos tokens: access token (JWT) e refresh token (opaco, guardado em hash no banco).
 */
@Validated
@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(
        @NotBlank(message = "Defina app.security.jwt.secret (variável JWT_SECRET)")
        @Size(min = 32, message = "app.security.jwt.secret precisa ter no mínimo 32 caracteres")
        String secret,
        @DefaultValue("4h") Duration accessTokenTtl,
        @DefaultValue("8h") Duration refreshTokenTtl) {
}
