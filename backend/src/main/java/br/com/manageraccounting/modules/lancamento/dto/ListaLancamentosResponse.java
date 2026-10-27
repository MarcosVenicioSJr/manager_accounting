package br.com.manageraccounting.modules.lancamento.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Página de lançamentos ({@code pagina} começa em 0) e os totais de tudo o que o filtro encontrou, não só da
 * página. {@code totais} traz os três tipos, sempre na mesma ordem, mesmo com zero.
 */
public record ListaLancamentosResponse(
        List<LancamentoResponse> itens,
        int pagina,
        int tamanho,
        long total,
        int totalPaginas,
        List<TotalTipoResponse> totais,
        BigDecimal valorTotal) {
}
