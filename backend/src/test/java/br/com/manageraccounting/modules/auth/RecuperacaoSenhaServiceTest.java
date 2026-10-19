package br.com.manageraccounting.modules.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import br.com.manageraccounting.core.config.AppWebProperties;
import br.com.manageraccounting.core.exception.BusinessException;
import br.com.manageraccounting.modules.tenant.Tenant;
import br.com.manageraccounting.modules.tenant.TenantRepository;
import br.com.manageraccounting.modules.usuario.Usuario;
import br.com.manageraccounting.modules.usuario.UsuarioRepository;

class RecuperacaoSenhaServiceTest {

    private static final Instant AGORA = Instant.parse("2026-10-19T21:00:00Z");
    private static final String SENHA_ATUAL = "Atual@1234";
    private static final String TOKEN = "token-de-teste-com-tamanho-suficiente-000000";

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final Clock clock = Clock.fixed(AGORA, ZoneOffset.UTC);

    private UsuarioRepository usuarioRepository;
    private TenantRepository tenantRepository;
    private TokenRedefinicaoSenhaRepository tokenRepository;
    private RefreshTokenService refreshTokenService;
    private ApplicationEventPublisher eventPublisher;
    private RecuperacaoSenhaService service;

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        tenantRepository = mock(TenantRepository.class);
        tokenRepository = mock(TokenRedefinicaoSenhaRepository.class);
        refreshTokenService = mock(RefreshTokenService.class);
        eventPublisher = mock(ApplicationEventPublisher.class);

        service = new RecuperacaoSenhaService(usuarioRepository, tenantRepository, tokenRepository,
                refreshTokenService, encoder, eventPublisher,
                new RecuperacaoSenhaProperties(Duration.ofMinutes(30), Duration.ofMinutes(2), 5),
                new AppWebProperties("https://app.exemplo.com/"), clock);

        when(tenantRepository.findById(10L)).thenReturn(Optional.of(tenant(true)));
    }

    // ---------- solicitar ----------

    @Test
    void solicitarGravaSoOHashDoTokenEEnviaLinkComOTokenNoFragmento() {
        when(usuarioRepository.findByLogin("contador")).thenReturn(Optional.of(usuario(true)));

        service.solicitar("  contador  ");

        ArgumentCaptor<TokenRedefinicaoSenha> salvo = ArgumentCaptor.forClass(TokenRedefinicaoSenha.class);
        verify(tokenRepository).save(salvo.capture());
        assertThat(salvo.getValue().getUsuarioId()).isEqualTo(1L);
        assertThat(salvo.getValue().getExpiraEm()).isEqualTo(AGORA.plus(Duration.ofMinutes(30)));
        verify(tokenRepository).invalidarPendentes(1L, AGORA);

        EmailPendente evento = emailPublicado();
        assertThat(evento.email().para()).isEqualTo("maria@exemplo.com");
        assertThat(evento.email().assunto()).isEqualTo(RecuperacaoSenhaEmails.ASSUNTO_LINK);

        String token = extrairToken(evento.email().texto());
        assertThat(token).hasSizeGreaterThanOrEqualTo(43);
        // no banco fica o hash, nunca o token em claro
        assertThat(salvo.getValue().getTokenHash()).isEqualTo(RefreshTokenService.hash(token)).isNotEqualTo(token);
        assertThat(evento.email().texto()).contains("https://app.exemplo.com/redefinir-senha#token=" + token);
    }

    @Test
    void solicitarParaLoginInexistenteNaoFazNada() {
        when(usuarioRepository.findByLogin("ninguem")).thenReturn(Optional.empty());

        service.solicitar("ninguem");

        verify(tokenRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void solicitarSemPrimeiroAcessoConcluidoNaoEnviaNada() {
        when(usuarioRepository.findByLogin("contador")).thenReturn(Optional.of(usuario(false)));

        service.solicitar("contador");

        verify(tokenRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void solicitarParaUsuarioInativoOuTenantInativoNaoEnviaNada() {
        Usuario inativo = usuario(true);
        ReflectionTestUtils.setField(inativo, "ativo", false);
        when(usuarioRepository.findByLogin("inativo")).thenReturn(Optional.of(inativo));
        service.solicitar("inativo");

        when(usuarioRepository.findByLogin("contador")).thenReturn(Optional.of(usuario(true)));
        when(tenantRepository.findById(10L)).thenReturn(Optional.of(tenant(false)));
        service.solicitar("contador");

        verify(tokenRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void solicitarDentroDoIntervaloMinimoNaoEnviaOutroEmail() {
        when(usuarioRepository.findByLogin("contador")).thenReturn(Optional.of(usuario(true)));
        when(tokenRepository.existsByUsuarioIdAndCriadoEmAfter(1L, AGORA.minus(Duration.ofMinutes(2))))
                .thenReturn(true);

        service.solicitar("contador");

        verify(tokenRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void solicitarAcimaDoLimitePorHoraNaoEnviaOutroEmail() {
        when(usuarioRepository.findByLogin("contador")).thenReturn(Optional.of(usuario(true)));
        when(tokenRepository.countByUsuarioIdAndCriadoEmAfter(1L, AGORA.minus(Duration.ofHours(1))))
                .thenReturn(5L);

        service.solicitar("contador");

        verify(tokenRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    // ---------- validar ----------

    @Test
    void validarAceitaTokenPendenteDentroDaValidade() {
        when(tokenRepository.findByTokenHash(RefreshTokenService.hash(TOKEN)))
                .thenReturn(Optional.of(token(AGORA.plus(Duration.ofMinutes(10)))));
        when(usuarioRepository.findAtivoById(1L)).thenReturn(Optional.of(usuario(true)));

        service.validar(TOKEN);
    }

    @Test
    void validarRecusaTokenExpiradoUsadoOuInexistente() {
        TokenRedefinicaoSenha expirado = token(AGORA.minusSeconds(1));
        TokenRedefinicaoSenha usado = token(AGORA.plus(Duration.ofMinutes(10)));
        usado.marcarComoUtilizado(AGORA.minusSeconds(30));
        when(tokenRepository.findByTokenHash(RefreshTokenService.hash("expirado"))).thenReturn(Optional.of(expirado));
        when(tokenRepository.findByTokenHash(RefreshTokenService.hash("usado"))).thenReturn(Optional.of(usado));
        when(usuarioRepository.findAtivoById(1L)).thenReturn(Optional.of(usuario(true)));

        for (String token : new String[] {"expirado", "usado", "inexistente", "", null}) {
            assertThatThrownBy(() -> service.validar(token))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(RecuperacaoSenhaService.MENSAGEM_LINK_INVALIDO);
        }
    }

    @Test
    void validarRecusaTokenDeUsuarioDesativado() {
        when(tokenRepository.findByTokenHash(RefreshTokenService.hash(TOKEN)))
                .thenReturn(Optional.of(token(AGORA.plus(Duration.ofMinutes(10)))));
        when(usuarioRepository.findAtivoById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.validar(TOKEN)).isInstanceOf(BusinessException.class);
    }

    // ---------- redefinir ----------

    @Test
    void redefinirTrocaASenhaConsomeOLinkTiraOBloqueioEDerrubaAsSessoes() {
        Usuario usuario = usuario(true);
        ReflectionTestUtils.setField(usuario, "tentativasFalhas", 3);
        ReflectionTestUtils.setField(usuario, "bloqueadoAte", AGORA.plus(Duration.ofMinutes(10)));
        TokenRedefinicaoSenha token = token(AGORA.plus(Duration.ofMinutes(10)));
        when(tokenRepository.findByTokenHashParaUso(RefreshTokenService.hash(TOKEN))).thenReturn(Optional.of(token));
        when(usuarioRepository.findAtivoById(1L)).thenReturn(Optional.of(usuario));

        service.redefinir(TOKEN, "Nova@Senha1");

        assertThat(encoder.matches("Nova@Senha1", usuario.getSenhaHash())).isTrue();
        assertThat(usuario.getTentativasFalhas()).isZero();
        assertThat(usuario.getBloqueadoAte()).isNull();
        assertThat(token.getUtilizadoEm()).isEqualTo(AGORA);
        assertThat(token.estaValido(AGORA)).isFalse();
        verify(tokenRepository).invalidarPendentes(1L, AGORA);
        verify(refreshTokenService).revogarTodos(1L);

        EmailPendente confirmacao = emailPublicado();
        assertThat(confirmacao.email().assunto()).isEqualTo(RecuperacaoSenhaEmails.ASSUNTO_CONFIRMACAO);
        assertThat(confirmacao.email().para()).isEqualTo("maria@exemplo.com");
        assertThat(confirmacao.email().texto()).contains("https://app.exemplo.com/login");
    }

    @Test
    void redefinirComSenhaForaDaPoliticaNaoAlteraNada() {
        Usuario usuario = usuario(true);
        String hashAntes = usuario.getSenhaHash();
        TokenRedefinicaoSenha token = token(AGORA.plus(Duration.ofMinutes(10)));
        when(tokenRepository.findByTokenHashParaUso(RefreshTokenService.hash(TOKEN))).thenReturn(Optional.of(token));
        when(usuarioRepository.findAtivoById(1L)).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> service.redefinir(TOKEN, "semnumero")).isInstanceOf(BusinessException.class);

        assertThat(usuario.getSenhaHash()).isEqualTo(hashAntes);
        assertThat(token.getUtilizadoEm()).isNull();
        verify(refreshTokenService, never()).revogarTodos(anyLong());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void redefinirComLinkJaUsadoEhRecusado() {
        TokenRedefinicaoSenha token = token(AGORA.plus(Duration.ofMinutes(10)));
        token.marcarComoUtilizado(AGORA.minusSeconds(5));
        when(tokenRepository.findByTokenHashParaUso(RefreshTokenService.hash(TOKEN))).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.redefinir(TOKEN, "Nova@Senha1"))
                .isInstanceOf(BusinessException.class)
                .hasMessage(RecuperacaoSenhaService.MENSAGEM_LINK_INVALIDO);
        verify(refreshTokenService, never()).revogarTodos(eq(1L));
    }

    // ---------- helpers ----------

    private EmailPendente emailPublicado() {
        ArgumentCaptor<EmailPendente> evento = ArgumentCaptor.forClass(EmailPendente.class);
        verify(eventPublisher).publishEvent(evento.capture());
        return evento.getValue();
    }

    private static String extrairToken(String texto) {
        int inicio = texto.indexOf("#token=") + "#token=".length();
        int fim = inicio;
        while (fim < texto.length() && !Character.isWhitespace(texto.charAt(fim))) {
            fim++;
        }
        return texto.substring(inicio, fim);
    }

    private TokenRedefinicaoSenha token(Instant expiraEm) {
        return new TokenRedefinicaoSenha(1L, RefreshTokenService.hash(TOKEN), AGORA.minus(Duration.ofMinutes(5)),
                expiraEm);
    }

    private Usuario usuario(boolean primeiroAcessoConcluido) {
        Usuario usuario = BeanUtils.instantiateClass(Usuario.class);
        ReflectionTestUtils.setField(usuario, "id", 1L);
        ReflectionTestUtils.setField(usuario, "tenantId", 10L);
        ReflectionTestUtils.setField(usuario, "login", "contador");
        ReflectionTestUtils.setField(usuario, "senhaHash", encoder.encode(SENHA_ATUAL));
        ReflectionTestUtils.setField(usuario, "ativo", true);
        ReflectionTestUtils.setField(usuario, "primeiroAcessoConcluido", primeiroAcessoConcluido);
        if (primeiroAcessoConcluido) {
            ReflectionTestUtils.setField(usuario, "nomeCompleto", "Maria Souza");
            ReflectionTestUtils.setField(usuario, "email", "maria@exemplo.com");
            ReflectionTestUtils.setField(usuario, "telefone", "85999991234");
        }
        return usuario;
    }

    private Tenant tenant(boolean ativo) {
        Tenant tenant = BeanUtils.instantiateClass(Tenant.class);
        ReflectionTestUtils.setField(tenant, "id", 10L);
        ReflectionTestUtils.setField(tenant, "nomeEmpresa", "Escritório Exemplo");
        ReflectionTestUtils.setField(tenant, "ativo", ativo);
        return tenant;
    }
}
