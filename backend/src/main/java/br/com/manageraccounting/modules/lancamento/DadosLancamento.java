package br.com.manageraccounting.modules.lancamento;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Dados já validados e limpos de um lançamento (o que a entidade aceita). */
record DadosLancamento(TipoLancamento tipo, LocalDate data, String descricao, BigDecimal valor,
        CategoriaLancamento categoria) {
}
