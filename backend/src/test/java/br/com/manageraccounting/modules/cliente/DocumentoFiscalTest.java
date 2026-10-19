package br.com.manageraccounting.modules.cliente;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import br.com.manageraccounting.core.exception.BusinessException;

class DocumentoFiscalTest {

    @Test
    void cpfValidoComOuSemPontuacao() {
        assertThat(DocumentoFiscal.normalizar("529.982.247-25", TipoPessoa.FISICA)).isEqualTo("52998224725");
        assertThat(DocumentoFiscal.normalizar(" 52998224725 ", TipoPessoa.FISICA)).isEqualTo("52998224725");
    }

    @ParameterizedTest
    @ValueSource(strings = { "52998224724", "11111111111", "5299822472", "529982247250", "5299822472A", "" })
    void cpfInvalido(String cpf) {
        assertThatThrownBy(() -> DocumentoFiscal.normalizar(cpf, TipoPessoa.FISICA))
                .isInstanceOf(BusinessException.class)
                .hasMessage("CPF inválido.");
    }

    @Test
    void cnpjNumerico() {
        assertThat(DocumentoFiscal.normalizar("11.222.333/0001-81", TipoPessoa.JURIDICA)).isEqualTo("11222333000181");
    }

    /** Exemplo oficial da Receita Federal para o CNPJ alfanumérico. */
    @Test
    void cnpjAlfanumericoEmMinusculasViraMaiusculas() {
        assertThat(DocumentoFiscal.normalizar("12.abc.345/01de-35", TipoPessoa.JURIDICA)).isEqualTo("12ABC34501DE35");
    }

    @ParameterizedTest
    @ValueSource(strings = { "11222333000182", "12ABC34501DE36", "00000000000000", "12ABC34501DEAB",
            "1122233300018", "52998224725" })
    void cnpjInvalido(String cnpj) {
        assertThatThrownBy(() -> DocumentoFiscal.normalizar(cnpj, TipoPessoa.JURIDICA))
                .isInstanceOf(BusinessException.class)
                .hasMessage("CNPJ inválido.");
    }

    @Test
    void documentoNulo() {
        assertThatThrownBy(() -> DocumentoFiscal.normalizar(null, TipoPessoa.FISICA))
                .isInstanceOf(BusinessException.class);
    }
}
