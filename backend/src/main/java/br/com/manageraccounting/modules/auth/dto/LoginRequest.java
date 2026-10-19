package br.com.manageraccounting.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Informe o login")
        @Size(max = 60, message = "Login muito longo")
        String login,

        @NotBlank(message = "Informe a senha")
        @Size(max = 128, message = "Senha muito longa")
        String senha,

        /** "Manter-me conectado": o cookie de refresh sobrevive ao fechamento do navegador. Opcional. */
        Boolean manterConectado) {
}
