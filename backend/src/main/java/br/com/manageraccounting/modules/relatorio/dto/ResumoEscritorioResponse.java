package br.com.manageraccounting.modules.relatorio.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import br.com.manageraccounting.modules.cliente.TipoPessoa;

/**
 * Resumo do escritório no período: uma linha por cliente com os totais por tipo.
 * Entram os clientes ativos (mesmo sem lançamento, com zero) e os inativos que tiveram lançamento no período.
 * Ordem: maior total primeiro, depois nome.
 *
 * @param clientesSemMovimento clientes ativos sem nenhum lançamento no período
 */
public record ResumoEscritorioResponse(
        String escritorio,
        LocalDate de,
        LocalDate ate,
        Instant geradoEm,
        List<Linha> linhas,
        Linha totais,
        long clientesComMovimento,
        long clientesSemMovimento) {

    /** {@code clienteId} e os dados do cliente ficam nulos na linha de totais. */
    public record Linha(Long clienteId, String nome, TipoPessoa tipoPessoa, String documento, Boolean ativo,
            long quantidade, BigDecimal compras, BigDecimal dividas, BigDecimal despesas, BigDecimal total) {
    }
}
