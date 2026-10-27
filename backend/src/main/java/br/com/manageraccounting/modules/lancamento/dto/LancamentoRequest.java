package br.com.manageraccounting.modules.lancamento.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import br.com.manageraccounting.modules.lancamento.CategoriaLancamento;
import br.com.manageraccounting.modules.lancamento.TipoLancamento;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Cadastro e edição de lançamento. O tenant nunca vem aqui (é o do token).
 * {@code clienteId} só vale no cadastro: um lançamento não muda de cliente. Na edição, {@code versao} é obrigatória
 * (a versão que a tela carregou) para não sobrescrever a alteração de outra pessoa.
 */
public record LancamentoRequest(
        Long clienteId,

        @NotNull(message = "Informe o tipo do lançamento")
        TipoLancamento tipo,

        @NotNull(message = "Informe a data")
        LocalDate data,

        @NotBlank(message = "Informe a descrição")
        @Size(max = 200, message = "Descrição muito longa (máximo de 200 caracteres)")
        String descricao,

        @NotNull(message = "Informe o valor")
        @DecimalMin(value = "0.01", message = "O valor precisa ser maior que zero")
        @DecimalMax(value = "999999999999.99", message = "Valor muito alto")
        @Digits(integer = 12, fraction = 2, message = "Use no máximo duas casas decimais")
        BigDecimal valor,

        @NotNull(message = "Informe a categoria")
        CategoriaLancamento categoria,

        Long versao) {
}
