package br.com.manageraccounting.core.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import br.com.manageraccounting.core.security.JwtService.AccessToken;
import br.com.manageraccounting.core.security.JwtService.TokenClaims;

class JwtServiceTest {

    private static final String SEGREDO = "0123456789012345678901234567890123456789";
    private static final Instant AGORA = Instant.parse("2026-10-15T12:00:00Z");

    private JwtService service(String segredo, Instant agora) {
        JwtProperties properties = new JwtProperties(segredo, Duration.ofHours(4), Duration.ofHours(8));
        return new JwtService(properties, Clock.fixed(agora, ZoneOffset.UTC));
    }

    @Test
    void tokenGeradoCarregaUserIdETenantId() {
        JwtService service = service(SEGREDO, AGORA);

        AccessToken token = service.generateAccessToken(7L, 3L);

        assertThat(token.expiresInSeconds()).isEqualTo(Duration.ofHours(4).toSeconds());
        assertThat(service.parse(token.value())).contains(new TokenClaims(7L, 3L));
    }

    @Test
    void tokenExpiraDepoisDeQuatroHoras() {
        AccessToken token = service(SEGREDO, AGORA).generateAccessToken(7L, 3L);

        assertThat(service(SEGREDO, AGORA.plus(Duration.ofHours(3))).parse(token.value())).isPresent();
        assertThat(service(SEGREDO, AGORA.plus(Duration.ofHours(5))).parse(token.value())).isEmpty();
    }

    @Test
    void tokenAssinadoComOutroSegredoEhRecusado() {
        AccessToken token = service("outro-segredo-com-mais-de-32-caracteres-xx", AGORA).generateAccessToken(7L, 3L);

        assertThat(service(SEGREDO, AGORA).parse(token.value())).isEmpty();
    }

    @Test
    void tokenAdulteradoOuMalformadoEhRecusado() {
        JwtService service = service(SEGREDO, AGORA);
        String[] original = service.generateAccessToken(7L, 3L).value().split("\\.");
        String[] outroUsuario = service.generateAccessToken(8L, 3L).value().split("\\.");
        // payload de outro usuário com a assinatura do primeiro: a assinatura não confere
        String adulterado = original[0] + "." + outroUsuario[1] + "." + original[2];

        assertThat(service.parse(adulterado)).isEmpty();
        assertThat(service.parse("isto-nao-e-um-jwt")).isEmpty();
        assertThat(service.parse("")).isEmpty();
    }
}
