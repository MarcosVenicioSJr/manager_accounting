package br.com.manageraccounting.modules.perfil.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Dados que mudam na hora, sem confirmação. */
public record AtualizarPerfilRequest(
        @NotBlank(message = "Informe o nome completo")
        @Size(max = 150, message = "Nome muito longo (máximo de 150 caracteres)")
        String nomeCompleto,

        @NotBlank(message = "Informe o telefone")
        @Size(max = 30, message = "Telefone muito longo")
        String telefone) {
}
