package br.com.manageraccounting.modules.perfil.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Token do link de confirmação. Vai no corpo, nunca na URL da API. */
public record ConfirmacaoTokenRequest(
        @NotBlank(message = "Link de confirmação inválido")
        @Size(max = 100, message = "Link de confirmação inválido")
        String token) {
}
