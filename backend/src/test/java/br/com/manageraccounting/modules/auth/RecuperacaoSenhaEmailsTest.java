package br.com.manageraccounting.modules.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import br.com.manageraccounting.core.mail.EmailMessage;

class RecuperacaoSenhaEmailsTest {

    private static final String LINK = "https://app.exemplo.com/redefinir-senha#token=abc_DEF-123";

    @Test
    void emailDoLinkTrazSaudacaoEscritorioValidadeELink() {
        EmailMessage email = RecuperacaoSenhaEmails.link("maria@exemplo.com", "Maria Souza", "Escritório Exemplo",
                LINK, 30);

        assertThat(email.para()).isEqualTo("maria@exemplo.com");
        assertThat(email.assunto()).isEqualTo("Redefinição de senha");
        assertThat(email.texto())
                .contains("Olá, Maria!")
                .contains("Escritório Exemplo")
                .contains("30 minutos")
                .contains(LINK);
        assertThat(email.html())
                .startsWith("<!doctype html>")
                .contains("href=\"" + LINK + "\"")
                .contains("Redefinir senha")
                .contains("width=\"100%\"");
    }

    @Test
    void nomesSaoEscapadosNoHtml() {
        EmailMessage email = RecuperacaoSenhaEmails.link("x@exemplo.com", "<script>", "A & B <Contábil>", LINK, 30);

        assertThat(email.html())
                .doesNotContain("<script>")
                .contains("&lt;script&gt;")
                .contains("A &amp; B &lt;Contábil&gt;");
    }

    @Test
    void semNomeUsaSaudacaoGenerica() {
        EmailMessage email = RecuperacaoSenhaEmails.confirmacao("x@exemplo.com", null, "Escritório",
                "https://app.exemplo.com/login");

        assertThat(email.assunto()).isEqualTo("Sua senha foi alterada");
        assertThat(email.texto()).startsWith("Olá!").contains("https://app.exemplo.com/login");
    }
}
