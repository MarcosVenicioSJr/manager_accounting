package br.com.manageraccounting.modules.lancamento.dto;

import java.math.BigDecimal;

import br.com.manageraccounting.modules.lancamento.TipoLancamento;

public record TotalTipoResponse(TipoLancamento tipo, long quantidade, BigDecimal valor) {
}
