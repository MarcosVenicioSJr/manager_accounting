package br.com.manageraccounting.modules.perfil.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TrocarEmailRequest(
        @NotBlank(message = "Informe a senha atual")
        @Size(max = 128, message = "Senha muito longa")
        String senhaAtual,

        @NotBlank(message = "Informe o novo e-mail")
        @Email(message = "E-mail inválido")
        @Size(max = 150, message = "E-mail muito longo")
        String novoEmail) {
}
