package br.com.manageraccounting.modules.perfil.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TrocarSenhaRequest(
        @NotBlank(message = "Informe a senha atual")
        @Size(max = 128, message = "Senha muito longa")
        String senhaAtual,

        @NotBlank(message = "Informe a nova senha")
        @Size(max = 128, message = "Senha muito longa")
        String novaSenha) {
}
