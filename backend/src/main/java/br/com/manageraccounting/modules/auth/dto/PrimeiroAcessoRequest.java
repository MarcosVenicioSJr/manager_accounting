package br.com.manageraccounting.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Dados do primeiro acesso: troca da senha temporária e dados adicionais do contador.
 */
public record PrimeiroAcessoRequest(
        @NotBlank(message = "Informe a nova senha")
        @Size(max = 128, message = "Senha muito longa")
        String novaSenha,

        @NotBlank(message = "Informe o nome completo")
        @Size(max = 150, message = "Nome muito longo")
        String nomeCompleto,

        @NotBlank(message = "Informe o e-mail")
        @Email(message = "E-mail inválido")
        @Size(max = 150, message = "E-mail muito longo")
        String email,

        @NotBlank(message = "Informe o telefone")
        @Size(max = 30, message = "Telefone muito longo")
        String telefone) {
}
