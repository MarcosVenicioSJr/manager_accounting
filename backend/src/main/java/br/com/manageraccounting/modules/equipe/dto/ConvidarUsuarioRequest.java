package br.com.manageraccounting.modules.equipe.dto;

import br.com.manageraccounting.modules.usuario.PerfilAcesso;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Convite de um novo usuário do escritório. O escritório é sempre o do administrador logado. */
public record ConvidarUsuarioRequest(
        @NotBlank(message = "Informe o nome completo")
        @Size(max = 150, message = "Nome muito longo (máximo de 150 caracteres)")
        String nomeCompleto,

        @NotBlank(message = "Informe o e-mail")
        @Email(message = "E-mail inválido")
        @Size(max = 150, message = "E-mail muito longo")
        String email,

        @NotBlank(message = "Informe o usuário de acesso")
        @Size(max = 60, message = "Usuário muito longo (máximo de 60 caracteres)")
        String login,

        @NotNull(message = "Informe o perfil")
        PerfilAcesso perfil) {
}
