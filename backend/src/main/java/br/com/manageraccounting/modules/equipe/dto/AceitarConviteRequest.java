package br.com.manageraccounting.modules.equipe.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Aceite do convite: a pessoa confere o nome, informa o telefone e cria a senha. */
public record AceitarConviteRequest(
        @NotBlank(message = "Convite inválido")
        @Size(max = 100, message = "Convite inválido")
        String token,

        @NotBlank(message = "Informe o nome completo")
        @Size(max = 150, message = "Nome muito longo (máximo de 150 caracteres)")
        String nomeCompleto,

        @NotBlank(message = "Informe o telefone")
        @Size(max = 30, message = "Telefone muito longo")
        String telefone,

        @NotBlank(message = "Informe a nova senha")
        @Size(max = 128, message = "Senha muito longa")
        String novaSenha) {
}
