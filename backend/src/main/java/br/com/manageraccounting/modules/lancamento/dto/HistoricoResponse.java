package br.com.manageraccounting.modules.lancamento.dto;

import java.time.Instant;

import br.com.manageraccounting.modules.lancamento.AcaoHistorico;
import br.com.manageraccounting.modules.lancamento.OrigemLancamento;
import br.com.manageraccounting.modules.lancamento.SnapshotLancamento;

/**
 * Uma ação do histórico, com o estado do lançamento logo depois dela. A tela compara com a linha anterior para
 * mostrar o que mudou.
 *
 * @param usuarioNome quem fez, no escritório; nulo quando foi o cliente pelo app
 */
public record HistoricoResponse(Long id, AcaoHistorico acao, OrigemLancamento origem, String usuarioNome,
        Instant feitoEm, SnapshotLancamento dados) {
}
