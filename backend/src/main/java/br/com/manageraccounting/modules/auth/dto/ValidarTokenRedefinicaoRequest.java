package br.com.manageraccounting.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** O token vai no corpo (não na URL) para não aparecer em logs de servidor e proxies. */
public record ValidarTokenRedefinicaoRequest(
        @NotBlank(message = "Link de redefinição inválido")
        @Size(max = 100, message = "Link de redefinição inválido")
        String token) {
}
