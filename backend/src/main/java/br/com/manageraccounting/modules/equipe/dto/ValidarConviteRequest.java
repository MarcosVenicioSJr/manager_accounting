package br.com.manageraccounting.modules.equipe.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Token do link do convite. Vai no corpo, nunca na URL da API. */
public record ValidarConviteRequest(
        @NotBlank(message = "Convite inválido")
        @Size(max = 100, message = "Convite inválido")
        String token) {
}
