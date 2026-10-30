package br.com.manageraccounting.core.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import br.com.manageraccounting.modules.usuario.PerfilAcesso;
import br.com.manageraccounting.modules.usuario.Usuario;
import br.com.manageraccounting.modules.usuario.UsuarioRepository;
import jakarta.servlet.FilterChain;

/**
 * Troca de senha: o access token emitido antes dela não pode continuar valendo até expirar.
 */
class JwtAuthenticationFilterTest {

    private static final String SEGREDO = "0123456789012345678901234567890123456789";
    private static final Instant EMITIDO = Instant.parse("2026-10-29T12:00:00Z");

    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);

    @AfterEach
    void limpar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void tokenValidoAutentica() {
        Usuario usuario = usuario();
        when(usuarioRepository.findAtivoById(1L)).thenReturn(Optional.of(usuario));

        assertThat(autenticar(token(EMITIDO), EMITIDO.plusSeconds(60))).isNotNull();
    }

    @Test
    void tokenEmitidoAntesDaTrocaDeSenhaEhRecusado() {
        Usuario usuario = usuario();
        usuario.encerrarSessoes(EMITIDO.plus(Duration.ofMinutes(5)));
        when(usuarioRepository.findAtivoById(1L)).thenReturn(Optional.of(usuario));

        assertThat(autenticar(token(EMITIDO), EMITIDO.plus(Duration.ofMinutes(10)))).isNull();
    }

    @Test
    void loginFeitoDepoisDaTrocaDeSenhaContinuaValendo() {
        Usuario usuario = usuario();
        // Troca às 12:05:00.700; o novo login no mesmo segundo gera iat 12:05:00 e precisa valer
        Instant troca = EMITIDO.plus(Duration.ofMinutes(5)).plusMillis(700);
        usuario.encerrarSessoes(troca);
        when(usuarioRepository.findAtivoById(1L)).thenReturn(Optional.of(usuario));

        assertThat(autenticar(token(troca), troca.plusSeconds(1))).isNotNull();
    }

    private Authentication autenticar(String token, Instant agora) {
        JwtService jwtService = jwtService(agora);
        JwtAuthenticationFilter filtro = new JwtAuthenticationFilter(jwtService, usuarioRepository);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/clientes");
        request.addHeader("Authorization", "Bearer " + token);
        AtomicReference<Authentication> durante = new AtomicReference<>();
        FilterChain chain = (req, res) -> durante.set(SecurityContextHolder.getContext().getAuthentication());
        try {
            filtro.doFilter(request, new MockHttpServletResponse(), chain);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
        return durante.get();
    }

    private static String token(Instant emitidoEm) {
        return jwtService(emitidoEm).generateAccessToken(1L, 10L).value();
    }

    private static JwtService jwtService(Instant agora) {
        return new JwtService(new JwtProperties(SEGREDO, Duration.ofHours(4), Duration.ofHours(8)),
                Clock.fixed(agora, ZoneOffset.UTC));
    }

    private static Usuario usuario() {
        Usuario usuario = Usuario.convidado(10L, "maria", "hash", "Maria", "maria@exemplo.com", PerfilAcesso.ADMIN,
                EMITIDO.minus(Duration.ofDays(30)));
        ReflectionTestUtils.setField(usuario, "id", 1L);
        ReflectionTestUtils.setField(usuario, "primeiroAcessoConcluido", true);
        return usuario;
    }
}
