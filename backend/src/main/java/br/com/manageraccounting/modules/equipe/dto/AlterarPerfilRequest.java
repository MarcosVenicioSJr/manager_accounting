package br.com.manageraccounting.modules.equipe.dto;

import br.com.manageraccounting.modules.usuario.PerfilAcesso;
import jakarta.validation.constraints.NotNull;

public record AlterarPerfilRequest(@NotNull(message = "Informe o perfil") PerfilAcesso perfil) {
}
