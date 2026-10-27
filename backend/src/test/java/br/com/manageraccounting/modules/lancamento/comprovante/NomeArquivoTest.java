package br.com.manageraccounting.modules.lancamento.comprovante;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NomeArquivoTest {

    @Test
    void tiraPastasECaracteresPerigosos() {
        assertThat(NomeArquivo.limpar("C:\\Users\\ana\\nota \"fiscal\".pdf", TipoComprovante.PDF))
                .isEqualTo("nota fiscal.pdf");
        assertThat(NomeArquivo.limpar("../../etc/passwd", TipoComprovante.PDF)).isEqualTo("passwd.pdf");
    }

    @Test
    void usaAExtensaoDoFormatoReal() {
        assertThat(NomeArquivo.limpar("foto.pdf", TipoComprovante.PNG)).isEqualTo("foto.png");
        assertThat(NomeArquivo.limpar("foto.JPEG", TipoComprovante.JPEG)).isEqualTo("foto.jpeg");
        assertThat(NomeArquivo.limpar("recibo", TipoComprovante.WEBP)).isEqualTo("recibo.webp");
    }

    @Test
    void semNomeAproveitavelViraComprovante() {
        assertThat(NomeArquivo.limpar(null, TipoComprovante.PDF)).isEqualTo("comprovante.pdf");
        assertThat(NomeArquivo.limpar("...", TipoComprovante.JPEG)).isEqualTo("comprovante.jpg");
        assertThat(NomeArquivo.limpar("pasta/", TipoComprovante.PNG)).isEqualTo("comprovante.png");
    }

    @Test
    void cortaNomesMuitoLongos() {
        String nome = NomeArquivo.limpar("a".repeat(500) + ".pdf", TipoComprovante.PDF);
        assertThat(nome).hasSize(NomeArquivo.TAMANHO_MAXIMO).endsWith(".pdf");
    }
}
