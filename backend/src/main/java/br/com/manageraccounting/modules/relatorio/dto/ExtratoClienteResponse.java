package br.com.manageraccounting.modules.relatorio.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import br.com.manageraccounting.modules.cliente.TipoPessoa;
import br.com.manageraccounting.modules.lancamento.CategoriaLancamento;
import br.com.manageraccounting.modules.lancamento.OrigemLancamento;
import br.com.manageraccounting.modules.lancamento.TipoLancamento;

/**
 * Extrato de um cliente: todos os lançamentos do período (mais antigos primeiro, como num extrato) e os totais.
 *
 * @param totaisPorTipo      os três tipos, sempre na mesma ordem, mesmo com zero
 * @param totaisPorCategoria só as categorias com lançamento, do maior valor para o menor
 */
public record ExtratoClienteResponse(
        String escritorio,
        ClienteRelatorio cliente,
        LocalDate de,
        LocalDate ate,
        Instant geradoEm,
        List<Item> itens,
        List<TotalTipo> totaisPorTipo,
        List<TotalCategoria> totaisPorCategoria,
        long quantidade,
        BigDecimal valorTotal) {

    public record ClienteRelatorio(Long id, String nome, String nomeFantasia, TipoPessoa tipoPessoa,
            String documento, boolean ativo) {
    }

    public record Item(Long id, LocalDate data, TipoLancamento tipo, String descricao, CategoriaLancamento categoria,
            String categoriaRotulo, BigDecimal valor, OrigemLancamento origem, boolean temComprovante) {
    }

    public record TotalTipo(TipoLancamento tipo, long quantidade, BigDecimal valor) {
    }

    public record TotalCategoria(CategoriaLancamento categoria, String rotulo, long quantidade, BigDecimal valor) {
    }
}
