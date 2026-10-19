package br.com.manageraccounting.modules.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import br.com.manageraccounting.core.security.RefreshCookieProperties;

class RefreshCookieFactoryTest {

    private final RefreshCookieFactory factory = new RefreshCookieFactory(new RefreshCookieProperties(true, "Strict"));

    @Test
    void manterConectadoGeraCookiePersistenteDeOitoHoras() {
        ResponseCookie cookie = factory.criar("token", Duration.ofHours(8), true);

        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofHours(8));
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getSameSite()).isEqualTo("Strict");
        assertThat(cookie.getPath()).isEqualTo("/api/auth");
    }

    @Test
    void semManterConectadoGeraCookieDeSessaoDoNavegador() {
        ResponseCookie cookie = factory.criar("token", Duration.ofHours(8), false);

        assertThat(cookie.getMaxAge().isNegative()).isTrue();
        assertThat(cookie.isHttpOnly()).isTrue();
    }

    @Test
    void expirarApagaOCookie() {
        assertThat(factory.expirar().getMaxAge()).isEqualTo(Duration.ZERO);
    }
}
