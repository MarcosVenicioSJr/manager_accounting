package br.com.manageraccounting.modules.relatorio.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import br.com.manageraccounting.modules.lancamento.CategoriaLancamento;
import br.com.manageraccounting.modules.relatorio.dto.ExtratoClienteResponse.ClienteRelatorio;

/**
 * Gastos por categoria no período, de um cliente ou de todos os clientes do escritório.
 * Só entram as categorias com lançamento, do maior total para o menor.
 *
 * @param cliente  nulo quando o relatório é de todos os clientes
 * @param totais   soma de todas as linhas ({@code categoria} e {@code rotulo} nulos)
 * @param clientes quantos clientes diferentes lançaram no período
 */
public record CategoriasResponse(
        String escritorio,
        ClienteRelatorio cliente,
        LocalDate de,
        LocalDate ate,
        Instant geradoEm,
        List<Linha> linhas,
        Linha totais,
        long clientes) {

    /**
     * @param percentual fatia do total do período, de 0 a 100, com uma casa
     * @param clientes   quantos clientes lançaram nessa categoria
     */
    public record Linha(CategoriaLancamento categoria, String rotulo, long quantidade, long clientes,
            BigDecimal compras, BigDecimal dividas, BigDecimal despesas, BigDecimal total, BigDecimal percentual) {
    }
}
