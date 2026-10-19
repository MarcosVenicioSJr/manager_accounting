package br.com.manageraccounting.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RedefinirSenhaRequest(
        @NotBlank(message = "Link de redefinição inválido")
        @Size(max = 100, message = "Link de redefinição inválido")
        String token,

        @NotBlank(message = "Informe a nova senha")
        @Size(max = 128, message = "Senha muito longa")
        String novaSenha) {
}
