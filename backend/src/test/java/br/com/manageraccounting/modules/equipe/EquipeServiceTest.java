package br.com.manageraccounting.modules.equipe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
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
import br.com.manageraccounting.core.exception.ConflictException;
import br.com.manageraccounting.core.exception.NotFoundException;
import br.com.manageraccounting.core.mail.EmailMessage;
import br.com.manageraccounting.modules.auth.EmailPendente;
import br.com.manageraccounting.modules.auth.RefreshTokenService;
import br.com.manageraccounting.modules.equipe.dto.ConvidarUsuarioRequest;
import br.com.manageraccounting.modules.equipe.dto.UsuarioEquipeResponse;
import br.com.manageraccounting.modules.equipe.dto.UsuarioEquipeResponse.Situacao;
import br.com.manageraccounting.modules.tenant.Tenant;
import br.com.manageraccounting.modules.tenant.TenantRepository;
import br.com.manageraccounting.modules.usuario.PerfilAcesso;
import br.com.manageraccounting.modules.usuario.Usuario;
import br.com.manageraccounting.modules.usuario.UsuarioRepository;

class EquipeServiceTest {

    private static final Instant AGORA = Instant.parse("2026-10-21T12:00:00Z");
    private static final Long TENANT = 10L;
    private static final Long ADMIN = 1L;

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);

    private UsuarioRepository usuarioRepository;
    private ConviteUsuarioRepository conviteRepository;
    private RefreshTokenService refreshTokenService;
    private ApplicationEventPublisher eventPublisher;
    private EquipeService service;
    private Usuario admin;

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        TenantRepository tenantRepository = mock(TenantRepository.class);
        conviteRepository = mock(ConviteUsuarioRepository.class);
        refreshTokenService = mock(RefreshTokenService.class);
        eventPublisher = mock(ApplicationEventPublisher.class);

        service = new EquipeService(usuarioRepository, tenantRepository, conviteRepository, refreshTokenService,
                encoder, eventPublisher, new EquipeProperties(Duration.ofHours(48), Duration.ofMinutes(1)),
                new AppWebProperties("https://app.exemplo.com"), Clock.fixed(AGORA, ZoneOffset.UTC));

        admin = usuario(ADMIN, PerfilAcesso.ADMIN, true, true);
        when(usuarioRepository.findByIdAndTenantId(ADMIN, TENANT)).thenReturn(Optional.of(admin));
        when(tenantRepository.findById(TENANT)).thenReturn(Optional.of(tenant()));
        when(conviteRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(usuarioRepository.saveAndFlush(any())).thenAnswer(i -> {
            Usuario u = i.getArgument(0);
            ReflectionTestUtils.setField(u, "id", 50L);
            return u;
        });
    }

    // ---------- convite ----------

    @Test
    void convidarCriaUsuarioPendenteNoEscritorioDoAdminEEnviaLink() {
        UsuarioEquipeResponse criado = service.convidar(TENANT, ADMIN,
                new ConvidarUsuarioRequest(" Ana Lima ", " Ana@Exemplo.com ", " Ana.Lima ", PerfilAcesso.USUARIO));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).saveAndFlush(captor.capture());
        Usuario novo = captor.getValue();
        assertThat(novo.getTenantId()).isEqualTo(TENANT);
        assertThat(novo.getLogin()).isEqualTo("ana.lima");
        assertThat(novo.getEmail()).isEqualTo("ana@exemplo.com");
        assertThat(novo.getNomeCompleto()).isEqualTo("Ana Lima");
        assertThat(novo.getPerfil()).isEqualTo(PerfilAcesso.USUARIO);
        assertThat(novo.isPrimeiroAcessoConcluido()).isFalse();
        assertThat(novo.isAtivo()).isTrue();
        assertThat(novo.getSenhaHash()).startsWith("$2");

        assertThat(criado.situacao()).isEqualTo(Situacao.PENDENTE);
        assertThat(criado.conviteExpiraEm()).isEqualTo(AGORA.plus(Duration.ofHours(48)));

        ArgumentCaptor<ConviteUsuario> convite = ArgumentCaptor.forClass(ConviteUsuario.class);
        verify(conviteRepository).save(convite.capture());
        EmailMessage email = email();
        assertThat(email.para()).isEqualTo("ana@exemplo.com");
        assertThat(email.texto()).contains("ana.lima").contains("Maria Admin");
        String token = email.texto().split("#token=")[1].split("\\s")[0];
        assertThat(convite.getValue().getTokenHash()).isEqualTo(EquipeService.sha256(token));
        assertThat(convite.getValue().getCriadoPor()).isEqualTo(ADMIN);
    }

    @Test
    void loginEmUso() {
        when(usuarioRepository.existsByLogin("ana.lima")).thenReturn(true);

        assertThatThrownBy(() -> service.convidar(TENANT, ADMIN,
                new ConvidarUsuarioRequest("Ana", "ana@x.com", "Ana.Lima", PerfilAcesso.USUARIO)))
                .isInstanceOf(ConflictException.class)
                .extracting("codigo").isEqualTo(EquipeService.CODIGO_LOGIN_EM_USO);
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void loginComCaracteresInvalidos() {
        for (String login : new String[] { "ab", "ana lima", "ana@lima", ".ana", "joão" }) {
            assertThatThrownBy(() -> service.convidar(TENANT, ADMIN,
                    new ConvidarUsuarioRequest("Ana", "ana@x.com", login, PerfilAcesso.USUARIO)))
                    .as(login)
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Test
    void reenviarAnulaOConviteAnterior() {
        Usuario pendente = usuario(5L, PerfilAcesso.USUARIO, true, false);
        when(usuarioRepository.findByIdAndTenantId(5L, TENANT)).thenReturn(Optional.of(pendente));

        service.reenviarConvite(TENANT, ADMIN, 5L);

        verify(conviteRepository).invalidarAbertos(5L, AGORA);
        verify(conviteRepository).save(any());
    }

    @Test
    void naoReenviaParaQuemJaCriouASenhaNemLogoEmSeguida() {
        Usuario ativo = usuario(5L, PerfilAcesso.USUARIO, true, true);
        Usuario pendente = usuario(6L, PerfilAcesso.USUARIO, true, false);
        when(usuarioRepository.findByIdAndTenantId(5L, TENANT)).thenReturn(Optional.of(ativo));
        when(usuarioRepository.findByIdAndTenantId(6L, TENANT)).thenReturn(Optional.of(pendente));
        when(conviteRepository.existsByUsuarioIdAndCriadoEmAfter(6L, AGORA.minus(Duration.ofMinutes(1))))
                .thenReturn(true);

        assertThatThrownBy(() -> service.reenviarConvite(TENANT, ADMIN, 5L)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.reenviarConvite(TENANT, ADMIN, 6L)).hasMessage(EquipeService.MSG_AGUARDE);
        verify(conviteRepository, never()).save(any());
    }

    @Test
    void cancelarConviteApagaOUsuarioPendente() {
        Usuario pendente = usuario(5L, PerfilAcesso.USUARIO, true, false);
        when(usuarioRepository.findByIdAndTenantId(5L, TENANT)).thenReturn(Optional.of(pendente));

        service.cancelarConvite(TENANT, ADMIN, 5L);

        verify(refreshTokenService).apagarTodos(5L);
        verify(usuarioRepository).delete(pendente);
    }

    @Test
    void naoCancelaConviteDeQuemJaCriouASenha() {
        when(usuarioRepository.findByIdAndTenantId(5L, TENANT))
                .thenReturn(Optional.of(usuario(5L, PerfilAcesso.USUARIO, true, true)));

        assertThatThrownBy(() -> service.cancelarConvite(TENANT, ADMIN, 5L)).isInstanceOf(BusinessException.class);
        verify(usuarioRepository, never()).delete(any());
    }

    // ---------- isolamento ----------

    @Test
    void usuarioDeOutroEscritorioRespondeNaoEncontrado() {
        when(usuarioRepository.findByIdAndTenantId(99L, TENANT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.desativar(TENANT, ADMIN, 99L)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.reativar(TENANT, ADMIN, 99L)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.alterarPerfil(TENANT, ADMIN, 99L, PerfilAcesso.ADMIN))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.reenviarConvite(TENANT, ADMIN, 99L)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.cancelarConvite(TENANT, ADMIN, 99L)).isInstanceOf(NotFoundException.class);
        verify(refreshTokenService, never()).revogarTodos(anyLong());
    }

    // ---------- perfil e situação ----------

    @Test
    void ninguemMudaOProprioPerfilNemSeDesativa() {
        assertThatThrownBy(() -> service.alterarPerfil(TENANT, ADMIN, ADMIN, PerfilAcesso.USUARIO))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.desativar(TENANT, ADMIN, ADMIN)).isInstanceOf(BusinessException.class);
        assertThat(admin.isAdmin()).isTrue();
        assertThat(admin.isAtivo()).isTrue();
    }

    @Test
    void escritorioNuncaFicaSemAdministrador() {
        Usuario outroAdmin = usuario(5L, PerfilAcesso.ADMIN, true, true);
        when(usuarioRepository.findByIdAndTenantId(5L, TENANT)).thenReturn(Optional.of(outroAdmin));
        when(usuarioRepository.contarAdminsEmAtividade(TENANT)).thenReturn(1L);

        assertThatThrownBy(() -> service.alterarPerfil(TENANT, ADMIN, 5L, PerfilAcesso.USUARIO))
                .hasMessage(EquipeService.MSG_ULTIMO_ADMIN);
        assertThatThrownBy(() -> service.desativar(TENANT, ADMIN, 5L)).hasMessage(EquipeService.MSG_ULTIMO_ADMIN);

        when(usuarioRepository.contarAdminsEmAtividade(TENANT)).thenReturn(2L);
        service.alterarPerfil(TENANT, ADMIN, 5L, PerfilAcesso.USUARIO);
        assertThat(outroAdmin.getPerfil()).isEqualTo(PerfilAcesso.USUARIO);
    }

    @Test
    void desativarEncerraSessoesEAnulaConvites() {
        Usuario usuario = usuario(5L, PerfilAcesso.USUARIO, true, true);
        when(usuarioRepository.findByIdAndTenantId(5L, TENANT)).thenReturn(Optional.of(usuario));

        service.desativar(TENANT, ADMIN, 5L);

        assertThat(usuario.isAtivo()).isFalse();
        verify(refreshTokenService).revogarTodos(5L);
        verify(conviteRepository).invalidarAbertos(5L, AGORA);

        service.reativar(TENANT, ADMIN, 5L);
        assertThat(usuario.isAtivo()).isTrue();
    }

    @Test
    void listaMostraSituacaoEQuemEVoce() {
        Usuario pendente = usuario(5L, PerfilAcesso.USUARIO, true, false);
        Usuario inativo = usuario(6L, PerfilAcesso.USUARIO, false, true);
        when(usuarioRepository.findByTenantIdOrderByAtivoDescNomeCompletoAscLoginAsc(TENANT))
                .thenReturn(List.of(admin, pendente, inativo));
        ConviteUsuario convite = new ConviteUsuario(5L, "h", ADMIN, AGORA, AGORA.plus(Duration.ofHours(10)));
        when(conviteRepository.findAbertos(List.of(1L, 5L, 6L), AGORA)).thenReturn(List.of(convite));

        List<UsuarioEquipeResponse> lista = service.listar(TENANT, ADMIN);

        assertThat(lista).extracting(UsuarioEquipeResponse::situacao)
                .containsExactly(Situacao.ATIVO, Situacao.PENDENTE, Situacao.INATIVO);
        assertThat(lista).extracting(UsuarioEquipeResponse::voce).containsExactly(true, false, false);
        assertThat(lista.get(1).conviteExpiraEm()).isEqualTo(AGORA.plus(Duration.ofHours(10)));
    }

    // ---------- aceite ----------

    @Test
    void aceitarCriaASenhaEConcluiOPrimeiroAcesso() {
        Usuario pendente = usuario(5L, PerfilAcesso.USUARIO, true, false);
        ConviteUsuario convite = new ConviteUsuario(5L, EquipeService.sha256("tok"), ADMIN, AGORA.minusSeconds(60),
                AGORA.plus(Duration.ofHours(1)));
        when(conviteRepository.findByTokenHashParaUso(EquipeService.sha256("tok"))).thenReturn(Optional.of(convite));
        when(usuarioRepository.findAtivoById(5L)).thenReturn(Optional.of(pendente));

        String login = service.aceitarConvite("tok", " Ana Lima ", "(85) 98888-7777", "Nova#5678x");

        assertThat(login).isEqualTo("usuario5");
        assertThat(pendente.isPrimeiroAcessoConcluido()).isTrue();
        assertThat(pendente.getNomeCompleto()).isEqualTo("Ana Lima");
        assertThat(pendente.getTelefone()).isEqualTo("85988887777");
        assertThat(encoder.matches("Nova#5678x", pendente.getSenhaHash())).isTrue();
        assertThat(convite.getAceitoEm()).isEqualTo(AGORA);
        verify(conviteRepository).invalidarAbertos(5L, AGORA);
    }

    @Test
    void conviteExpiradoOuDeQuemJaConcluiu() {
        ConviteUsuario expirado = new ConviteUsuario(5L, "h", ADMIN, AGORA.minusSeconds(999), AGORA.minusSeconds(1));
        when(conviteRepository.findByTokenHash(EquipeService.sha256("expirado"))).thenReturn(Optional.of(expirado));
        ConviteUsuario valido = new ConviteUsuario(7L, "h2", ADMIN, AGORA, AGORA.plus(Duration.ofHours(1)));
        when(conviteRepository.findByTokenHash(EquipeService.sha256("concluido"))).thenReturn(Optional.of(valido));
        when(usuarioRepository.findAtivoById(7L)).thenReturn(Optional.of(usuario(7L, PerfilAcesso.USUARIO, true, true)));

        assertThatThrownBy(() -> service.validarConvite("expirado")).hasMessage(EquipeService.MSG_CONVITE_INVALIDO);
        assertThatThrownBy(() -> service.validarConvite("concluido")).hasMessage(EquipeService.MSG_CONVITE_INVALIDO);
        assertThatThrownBy(() -> service.validarConvite("nao-existe")).hasMessage(EquipeService.MSG_CONVITE_INVALIDO);
    }

    // ---------- apoio ----------

    private EmailMessage email() {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(captor.capture());
        return ((EmailPendente) captor.getValue()).email();
    }

    private Usuario usuario(Long id, PerfilAcesso perfil, boolean ativo, boolean primeiroAcessoConcluido) {
        Usuario u = BeanUtils.instantiateClass(Usuario.class);
        ReflectionTestUtils.setField(u, "id", id);
        ReflectionTestUtils.setField(u, "tenantId", TENANT);
        ReflectionTestUtils.setField(u, "login", id.equals(ADMIN) ? "maria.admin" : "usuario" + id);
        ReflectionTestUtils.setField(u, "senhaHash", "x");
        ReflectionTestUtils.setField(u, "nomeCompleto", id.equals(ADMIN) ? "Maria Admin" : "Usuário " + id);
        ReflectionTestUtils.setField(u, "email", "u" + id + "@exemplo.com");
        ReflectionTestUtils.setField(u, "perfil", perfil);
        ReflectionTestUtils.setField(u, "ativo", ativo);
        ReflectionTestUtils.setField(u, "primeiroAcessoConcluido", primeiroAcessoConcluido);
        return u;
    }

    private static Tenant tenant() {
        Tenant t = BeanUtils.instantiateClass(Tenant.class);
        ReflectionTestUtils.setField(t, "id", TENANT);
        ReflectionTestUtils.setField(t, "nomeEmpresa", "ILLUM Contabilidade");
        ReflectionTestUtils.setField(t, "ativo", true);
        return t;
    }
}
