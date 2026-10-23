package br.com.manageraccounting.modules.perfil.dto;

import br.com.manageraccounting.modules.perfil.TipoAlteracao;

/** O que o link confirma, para a tela mostrar antes do clique em "Confirmar". */
public record ConfirmacaoInfoResponse(TipoAlteracao tipo, String novoEmail) {
}
