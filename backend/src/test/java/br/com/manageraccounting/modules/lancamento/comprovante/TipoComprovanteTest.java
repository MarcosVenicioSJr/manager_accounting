package br.com.manageraccounting.modules.lancamento.comprovante;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TipoComprovanteTest {

    @Test
    void reconheceOsFormatosAceitosPelosPrimeirosBytes() {
        assertThat(TipoComprovante.detectar("%PDF-1.4 resto".getBytes())).contains(TipoComprovante.PDF);
        assertThat(TipoComprovante.detectar(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0}))
                .contains(TipoComprovante.JPEG);
        assertThat(TipoComprovante.detectar(
                new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A})).contains(TipoComprovante.PNG);
        assertThat(TipoComprovante.detectar("RIFF\0\0\0\0WEBPVP8 ".getBytes())).contains(TipoComprovante.WEBP);
    }

    @Test
    void recusaOQueNaoEhComprovante() {
        assertThat(TipoComprovante.detectar("<html><script>".getBytes())).isEmpty();
        assertThat(TipoComprovante.detectar("RIFF\0\0\0\0WAVEfmt ".getBytes())).isEmpty();
        assertThat(TipoComprovante.detectar("%PD".getBytes())).isEmpty();
        assertThat(TipoComprovante.detectar(new byte[0])).isEmpty();
        assertThat(TipoComprovante.detectar(null)).isEmpty();
    }
}
