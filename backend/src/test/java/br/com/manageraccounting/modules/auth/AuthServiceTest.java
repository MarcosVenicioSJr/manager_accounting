package br.com.manageraccounting.modules.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import br.com.manageraccounting.core.exception.BusinessException;
import br.com.manageraccounting.core.exception.UnauthorizedException;
import br.com.manageraccounting.core.security.JwtProperties;
import br.com.manageraccounting.core.security.JwtService;
import br.com.manageraccounting.modules.auth.dto.PrimeiroAcessoRequest;
import br.com.manageraccounting.modules.tenant.Tenant;
import br.com.manageraccounting.modules.tenant.TenantRepository;
import br.com.manageraccounting.modules.usuario.Usuario;
import br.com.manageraccounting.modules.usuario.UsuarioRepository;

class AuthServiceTest {

    private static final Instant AGORA = Instant.parse("2026-10-15T12:00:00Z");
    private static final String SENHA_TEMPORARIA = "Temp@1234";

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final Clock clock = Clock.fixed(AGORA, ZoneOffset.UTC);

    private UsuarioRepository usuarioRepository;
    private TenantRepository tenantRepository;
    private RefreshTokenService refreshTokenService;
    private AuthService service;

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        tenantRepository = mock(TenantRepository.class);
        refreshTokenService = mock(RefreshTokenService.class);

        JwtProperties properties = new JwtProperties(
                "0123456789012345678901234567890123456789", Duration.ofHours(4), Duration.ofHours(8));
        service = new AuthService(usuarioRepository, tenantRepository, encoder,
                new JwtService(properties, clock), refreshTokenService, clock);

        when(tenantRepository.findById(10L)).thenReturn(Optional.of(tenant(true)));
        when(refreshTokenService.criar(1L)).thenReturn("refresh-token");
    }

    // ---------- login ----------

    @Test
    void loginComSenhaTemporariaIndicaPrimeiroAcessoPendente() {
        when(usuarioRepository.findByLogin("contador")).thenReturn(Optional.of(usuario(false)));

        LoginResult result = service.login("contador", SENHA_TEMPORARIA);

        assertThat(result.response().primeiroAcessoPendente()).isTrue();
        assertThat(result.response().accessToken()).isNotBlank();
        assertThat(result.response().expiresIn()).isEqualTo(Duration.ofHours(4).toSeconds());
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
    }

    @Test
    void loginDeUsuarioComPrimeiroAcessoConcluidoNaoTemPendencia() {
        when(usuarioRepository.findByLogin("contador")).thenReturn(Optional.of(usuario(true)));

        LoginResult result = service.login("contador", SENHA_TEMPORARIA);

        assertThat(result.response().primeiroAcessoPendente()).isFalse();
    }

    @Test
    void loginInexistenteRetornaMensagemGenerica() {
        when(usuarioRepository.findByLogin("ninguem")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login("ninguem", SENHA_TEMPORARIA))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage(AuthService.MENSAGEM_LOGIN_INVALIDO);
    }

    @Test
    void senhaErradaRetornaAMesmaMensagemGenericaEContaATentativa() {
        Usuario usuario = usuario(true);
        when(usuarioRepository.findByLogin("contador")).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> service.login("contador", "errada@123"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage(AuthService.MENSAGEM_LOGIN_INVALIDO);
        assertThat(usuario.getTentativasFalhas()).isEqualTo(1);
    }

    @Test
    void bloqueiaOUsuarioAposCincoSenhasErradasMesmoQueDepoisACertaSejaInformada() {
        Usuario usuario = usuario(true);
        when(usuarioRepository.findByLogin("contador")).thenReturn(Optional.of(usuario));

        for (int i = 0; i < AuthService.MAX_TENTATIVAS; i++) {
            assertThatThrownBy(() -> service.login("contador", "errada@123"))
                    .isInstanceOf(UnauthorizedException.class);
        }

        assertThat(usuario.estaBloqueado(AGORA)).isTrue();
        assertThatThrownBy(() -> service.login("contador", SENHA_TEMPORARIA))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage(AuthService.MENSAGEM_LOGIN_INVALIDO);
    }

    @Test
    void loginComSucessoZeraAsTentativasErradas() {
        Usuario usuario = usuario(true);
        when(usuarioRepository.findByLogin("contador")).thenReturn(Optional.of(usuario));
        assertThatThrownBy(() -> service.login("contador", "errada@123")).isInstanceOf(UnauthorizedException.class);

        service.login("contador", SENHA_TEMPORARIA);

        assertThat(usuario.getTentativasFalhas()).isZero();
    }

    @Test
    void tenantInativoNaoPodeFazerLogin() {
        when(tenantRepository.findById(10L)).thenReturn(Optional.of(tenant(false)));
        when(usuarioRepository.findByLogin("contador")).thenReturn(Optional.of(usuario(true)));

        assertThatThrownBy(() -> service.login("contador", SENHA_TEMPORARIA))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage(AuthService.MENSAGEM_LOGIN_INVALIDO);
    }

    @Test
    void usuarioInativoNaoPodeFazerLogin() {
        Usuario usuario = usuario(true);
        ReflectionTestUtils.setField(usuario, "ativo", false);
        when(usuarioRepository.findByLogin("contador")).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> service.login("contador", SENHA_TEMPORARIA))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void senhaMaiorQueOLimiteDoBcryptEhTratadaComoInvalida() {
        when(usuarioRepository.findByLogin("contador")).thenReturn(Optional.of(usuario(true)));

        assertThatThrownBy(() -> service.login("contador", "x".repeat(100)))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage(AuthService.MENSAGEM_LOGIN_INVALIDO);
    }

    // ---------- refresh ----------

    @Test
    void refreshSemSessaoValidaRetorna401() {
        when(refreshTokenService.buscarValido("token-invalido")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refresh("token-invalido"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage(AuthService.MENSAGEM_SESSAO_INVALIDA);
    }

    @Test
    void refreshComSessaoValidaGeraNovoAccessToken() {
        RefreshToken sessao = new RefreshToken(1L, "hash", AGORA, AGORA.plus(Duration.ofHours(8)));
        when(refreshTokenService.buscarValido("token-ok")).thenReturn(Optional.of(sessao));
        when(usuarioRepository.findAtivoById(1L)).thenReturn(Optional.of(usuario(true)));

        assertThat(service.refresh("token-ok").accessToken()).isNotBlank();
    }

    // ---------- primeiro acesso ----------

    @Test
    void primeiroAcessoSubstituiASenhaTemporariaEGravaOsDados() {
        Usuario usuario = usuario(false);
        when(usuarioRepository.findAtivoById(1L)).thenReturn(Optional.of(usuario));

        service.concluirPrimeiroAcesso(1L, new PrimeiroAcessoRequest(
                "Nova@Senha1", "  Maria Souza ", "Maria@Exemplo.com", "(85) 99999-1234"));

        assertThat(usuario.isPrimeiroAcessoConcluido()).isTrue();
        assertThat(encoder.matches("Nova@Senha1", usuario.getSenhaHash())).isTrue();
        assertThat(encoder.matches(SENHA_TEMPORARIA, usuario.getSenhaHash())).isFalse();
        assertThat(usuario.getNomeCompleto()).isEqualTo("Maria Souza");
        assertThat(usuario.getEmail()).isEqualTo("maria@exemplo.com");
        assertThat(usuario.getTelefone()).isEqualTo("85999991234");
    }

    @Test
    void primeiroAcessoRecusaSenhaForaDaPolitica() {
        when(usuarioRepository.findAtivoById(1L)).thenReturn(Optional.of(usuario(false)));

        assertThatThrownBy(() -> service.concluirPrimeiroAcesso(1L, new PrimeiroAcessoRequest(
                "fraca", "Maria Souza", "maria@exemplo.com", "85999991234")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void primeiroAcessoRecusaManterASenhaTemporaria() {
        when(usuarioRepository.findAtivoById(1L)).thenReturn(Optional.of(usuario(false)));

        assertThatThrownBy(() -> service.concluirPrimeiroAcesso(1L, new PrimeiroAcessoRequest(
                SENHA_TEMPORARIA, "Maria Souza", "maria@exemplo.com", "85999991234")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("diferente");
    }

    @Test
    void primeiroAcessoRecusaTelefoneInvalido() {
        when(usuarioRepository.findAtivoById(1L)).thenReturn(Optional.of(usuario(false)));

        assertThatThrownBy(() -> service.concluirPrimeiroAcesso(1L, new PrimeiroAcessoRequest(
                "Nova@Senha1", "Maria Souza", "maria@exemplo.com", "123")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Telefone");
    }

    @Test
    void primeiroAcessoNaoPodeSerRepetido() {
        when(usuarioRepository.findAtivoById(1L)).thenReturn(Optional.of(usuario(true)));

        assertThatThrownBy(() -> service.concluirPrimeiroAcesso(1L, new PrimeiroAcessoRequest(
                "Nova@Senha1", "Maria Souza", "maria@exemplo.com", "85999991234")))
                .isInstanceOf(BusinessException.class);
    }

    // ---------- helpers ----------

    private Usuario usuario(boolean primeiroAcessoConcluido) {
        Usuario usuario = BeanUtils.instantiateClass(Usuario.class);
        ReflectionTestUtils.setField(usuario, "id", 1L);
        ReflectionTestUtils.setField(usuario, "tenantId", 10L);
        ReflectionTestUtils.setField(usuario, "login", "contador");
        ReflectionTestUtils.setField(usuario, "senhaHash", encoder.encode(SENHA_TEMPORARIA));
        ReflectionTestUtils.setField(usuario, "ativo", true);
        ReflectionTestUtils.setField(usuario, "primeiroAcessoConcluido", primeiroAcessoConcluido);
        return usuario;
    }

    private Tenant tenant(boolean ativo) {
        Tenant tenant = BeanUtils.instantiateClass(Tenant.class);
        ReflectionTestUtils.setField(tenant, "id", 10L);
        ReflectionTestUtils.setField(tenant, "ativo", ativo);
        return tenant;
    }
}
