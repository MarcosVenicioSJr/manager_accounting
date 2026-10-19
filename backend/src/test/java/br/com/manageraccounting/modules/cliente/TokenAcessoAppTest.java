package br.com.manageraccounting.modules.cliente;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class TokenAcessoAppTest {

    @Test
    void formatoComQuatroGruposDoAlfabetoCrockford() {
        String token = TokenAcessoApp.gerar();
        assertThat(token).matches("[0-9A-HJKMNP-TV-Z]{4}(-[0-9A-HJKMNP-TV-Z]{4}){3}");
    }

    @Test
    void tokensNaoSeRepetem() {
        Set<String> tokens = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            tokens.add(TokenAcessoApp.gerar());
        }
        assertThat(tokens).hasSize(1000);
    }

    @Test
    void hashIgnoraHifensEspacosMaiusculasEConfusoesDeDigitacao() {
        String hash = TokenAcessoApp.hash("AB01-CD1E-FG0H-JK1M");
        assertThat(TokenAcessoApp.hash("ab01cd1efg0hjk1m")).isEqualTo(hash);
        assertThat(TokenAcessoApp.hash(" AB01 CD1E FG0H JK1M ")).isEqualTo(hash);
        // O no lugar de 0, I e L no lugar de 1
        assertThat(TokenAcessoApp.hash("ABO1-CDIE-FGOH-JKLM")).isEqualTo(hash);
        assertThat(hash).hasSize(64).isNotEqualTo(TokenAcessoApp.hash("AB01-CD1E-FG0H-JK1N"));
    }
}
