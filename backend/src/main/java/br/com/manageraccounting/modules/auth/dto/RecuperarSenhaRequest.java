package br.com.manageraccounting.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Pedido do link de redefinição: o usuário informa o mesmo login da tela de entrada. */
public record RecuperarSenhaRequest(
        @NotBlank(message = "Informe o usuário")
        @Size(max = 60, message = "Usuário muito longo")
        String login) {
}
