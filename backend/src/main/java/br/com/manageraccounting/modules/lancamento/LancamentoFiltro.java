package br.com.manageraccounting.modules.lancamento;

import java.time.LocalDate;

/**
 * Filtros da lista de lançamentos. Todos opcionais; o tenant não entra aqui (vem do token, à parte).
 *
 * @param busca procura na descrição, sem diferenciar acentos nem maiúsculas
 */
public record LancamentoFiltro(Long clienteId, TipoLancamento tipo, CategoriaLancamento categoria,
        LocalDate de, LocalDate ate, String busca) {

    public static LancamentoFiltro vazio() {
        return new LancamentoFiltro(null, null, null, null, null, null);
    }
}
