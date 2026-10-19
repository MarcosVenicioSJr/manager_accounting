package br.com.manageraccounting.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

/**
 * Endereço público do front (backoffice). Usado para montar os links enviados por e-mail.
 */
@Validated
@ConfigurationProperties(prefix = "app.web")
public record AppWebProperties(@DefaultValue("http://localhost:5173") @NotBlank String baseUrl) {

    /** Base sem a barra final, para concatenar caminhos. */
    public String baseUrlSemBarra() {
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
