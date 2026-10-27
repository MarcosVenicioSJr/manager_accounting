package br.com.manageraccounting.modules.lancamento.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import br.com.manageraccounting.modules.lancamento.CategoriaLancamento;
import br.com.manageraccounting.modules.lancamento.OrigemLancamento;
import br.com.manageraccounting.modules.lancamento.TipoLancamento;

/**
 * @param comprovante nulo quando não há arquivo anexado
 * @param criadoPor   nome de quem lançou no escritório; nulo quando foi o cliente pelo app
 * @param editadoEm   preenchido quando o lançamento mudou depois de criado (a tela mostra "Editado")
 */
public record LancamentoResponse(
        Long id,
        Long clienteId,
        String clienteNome,
        TipoLancamento tipo,
        LocalDate data,
        String descricao,
        BigDecimal valor,
        CategoriaLancamento categoria,
        OrigemLancamento origem,
        ComprovanteResponse comprovante,
        Instant criadoEm,
        String criadoPor,
        Instant editadoEm,
        long versao) {
}
