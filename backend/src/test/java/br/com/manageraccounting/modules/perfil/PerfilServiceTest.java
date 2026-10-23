package br.com.manageraccounting.modules.perfil;

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
import br.com.manageraccounting.core.mail.EmailMessage;
import br.com.manageraccounting.modules.auth.EmailPendente;
import br.com.manageraccounting.modules.auth.RefreshTokenService;
import br.com.manageraccounting.modules.auth.TokenRedefinicaoSenhaRepository;
import br.com.manageraccounting.modules.perfil.dto.AtualizarPerfilRequest;
import br.com.manageraccounting.modules.perfil.dto.ConfirmacaoInfoResponse;
import br.com.manageraccounting.modules.perfil.dto.PerfilResponse;
import br.com.manageraccounting.modules.perfil.dto.PerfilResponse.TrocaPendente;
import br.com.manageraccounting.modules.tenant.Tenant;
import br.com.manageraccounting.modules.tenant.TenantRepository;
import br.com.manageraccounting.modules.usuario.Usuario;
import br.com.manageraccounting.modules.usuario.UsuarioRepository;

class PerfilServiceTest {

    private static final Instant AGORA = Instant.parse("2026-10-21T12:00:00Z");
    private static final String SENHA_ATUAL = "Atual@1234";
    private static final String NOVA_SENHA = "Nova#5678x";

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final Clock clock = Clock.fixed(AGORA, ZoneOffset.UTC);

    private UsuarioRepository usuarioRepository;
    private AlteracaoPendenteRepository alteracaoRepository;
    private TokenRedefinicaoSenhaRepository tokenRedefinicaoRepository;
    private RefreshTokenService refreshTokenService;
    private ApplicationEventPublisher eventPublisher;
    private PerfilService service;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        TenantRepository tenantRepository = mock(TenantRepository.class);
        alteracaoRepository = mock(AlteracaoPendenteRepository.class);
        tokenRedefinicaoRepository = mock(TokenRedefinicaoSenhaRepository.class);
        refreshTokenService = mock(RefreshTokenService.class);
        eventPublisher = mock(ApplicationEventPublisher.class);

        service = new PerfilService(usuarioRepository, tenantRepository, alteracaoRepository,
                tokenRedefinicaoRepository, refreshTokenService, encoder, eventPublisher,
                new PerfilProperties(Duration.ofMinutes(30), Duration.ofMinutes(1), 5),
                new AppWebProperties("https://app.exemplo.com/"), clock);

        usuario = usuario();
        when(usuarioRepository.findAtivoById(1L)).thenReturn(Optional.of(usuario));
        when(tenantRepository.findById(10L)).thenReturn(Optional.of(tenant()));
        when(alteracaoRepository.findAbertas(anyLong(), any())).thenReturn(List.of());
    }

    // ---------- dados ----------

    @Test
    void atualizaNomeETelefoneNaHora() {
        PerfilResponse resposta = service.atualizarDados(1L,
                new AtualizarPerfilRequest("  Maria Souza Lima ", "(85) 98888-7777"));

        assertThat(usuario.getNomeCompleto()).isEqualTo("Maria Souza Lima");
        assertThat(usuario.getTelefone()).isEqualTo("85988887777");
        assertThat(resposta.nomeEscritorio()).isEqualTo("ILLUM Contabilidade");
        assertThat(resposta.email()).isEqualTo("maria@exemplo.com");
    }

    @Test
    void telefoneSemDdd() {
        assertThatThrownBy(() -> service.atualizarDados(1L, new AtualizarPerfilRequest("Maria", "98888-777")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Telefone inválido");
    }

    // ---------- troca de senha: pedido ----------

    @Test
    void pedidoDeTrocaDeSenhaNaoTrocaASenhaEEnviaLinkParaOEmailAtual() {
        String hashAntes = usuario.getSenhaHash();

        TrocaPendente resposta = service.solicitarTrocaSenha(1L, SENHA_ATUAL, NOVA_SENHA);

        assertThat(usuario.getSenhaHash()).isEqualTo(hashAntes);
        assertThat(resposta.enviadoPara()).isEqualTo("maria@exemplo.com");
        assertThat(resposta.expiraEm()).isEqualTo(AGORA.plus(Duration.ofMinutes(30)));

        AlteracaoPendente salva = pendenteSalva();
        assertThat(salva.getTipo()).isEqualTo(TipoAlteracao.SENHA);
        assertThat(encoder.matches(NOVA_SENHA, salva.getValorNovo())).isTrue();
        assertThat(salva.getReferencia()).isEqualTo(PerfilService.sha256(hashAntes));
        verify(alteracaoRepository).invalidarAbertas(1L, TipoAlteracao.SENHA, AGORA);

        EmailMessage email = emailPublicado();
        assertThat(email.para()).isEqualTo("maria@exemplo.com");
        assertThat(email.assunto()).isEqualTo(PerfilEmails.ASSUNTO_CONFIRMAR_SENHA);
        String token = tokenDoLink(email.texto());
        assertThat(salva.getTokenHash()).isEqualTo(PerfilService.sha256(token)).doesNotContain(token);
    }

    @Test
    void senhaAtualErradaContaComoTentativaENaoEnvia() {
        assertThatThrownBy(() -> service.solicitarTrocaSenha(1L, "errada", NOVA_SENHA))
                .isInstanceOf(SenhaAtualIncorretaException.class)
                .hasMessage(PerfilService.MSG_SENHA_ATUAL);

        assertThat(usuario.getTentativasFalhas()).isEqualTo(1);
        verify(alteracaoRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void cincoSenhasErradasBloqueiam() {
        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> service.solicitarTrocaSenha(1L, "errada", NOVA_SENHA))
                    .hasMessage(PerfilService.MSG_SENHA_ATUAL);
        }
        assertThatThrownBy(() -> service.solicitarTrocaSenha(1L, "errada", NOVA_SENHA))
                .hasMessage(PerfilService.MSG_BLOQUEADO);
        // Bloqueado, nem a senha certa passa
        assertThatThrownBy(() -> service.solicitarTrocaSenha(1L, SENHA_ATUAL, NOVA_SENHA))
                .hasMessage(PerfilService.MSG_BLOQUEADO);
        assertThat(usuario.getBloqueadoAte()).isEqualTo(AGORA.plus(Duration.ofMinutes(15)));
    }

    @Test
    void novaSenhaForaDaPoliticaOuIgualAAtual() {
        assertThatThrownBy(() -> service.solicitarTrocaSenha(1L, SENHA_ATUAL, "fraca"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.solicitarTrocaSenha(1L, SENHA_ATUAL, SENHA_ATUAL))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("diferente");
        verify(alteracaoRepository, never()).save(any());
    }

    @Test
    void pedidoRepetidoLogoEmSeguidaEhRecusado() {
        when(alteracaoRepository.existsByUsuarioIdAndTipoAndCriadoEmAfter(1L, TipoAlteracao.SENHA,
                AGORA.minus(Duration.ofMinutes(1)))).thenReturn(true);

        assertThatThrownBy(() -> service.solicitarTrocaSenha(1L, SENHA_ATUAL, NOVA_SENHA))
                .hasMessage(PerfilService.MSG_AGUARDE);
        verify(alteracaoRepository, never()).save(any());
    }

    // ---------- troca de e-mail: pedido ----------

    @Test
    void pedidoDeTrocaDeEmailEnviaLinkParaONovoEnderecoSemTrocarAinda() {
        TrocaPendente resposta = service.solicitarTrocaEmail(1L, SENHA_ATUAL, "  Maria.Nova@Exemplo.com ");

        assertThat(usuario.getEmail()).isEqualTo("maria@exemplo.com");
        assertThat(resposta.novoEmail()).isEqualTo("maria.nova@exemplo.com");
        AlteracaoPendente salva = pendenteSalva();
        assertThat(salva.getTipo()).isEqualTo(TipoAlteracao.EMAIL);
        assertThat(salva.getValorNovo()).isEqualTo("maria.nova@exemplo.com");
        assertThat(salva.getReferencia()).isEqualTo("maria@exemplo.com");

        EmailMessage email = emailPublicado();
        assertThat(email.para()).isEqualTo("maria.nova@exemplo.com");
        assertThat(email.assunto()).isEqualTo(PerfilEmails.ASSUNTO_CONFIRMAR_EMAIL);
    }

    @Test
    void novoEmailIgualAoAtual() {
        assertThatThrownBy(() -> service.solicitarTrocaEmail(1L, SENHA_ATUAL, "MARIA@exemplo.com"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("e-mail atual");
    }

    // ---------- confirmação ----------

    @Test
    void confirmarTrocaDeSenhaAplicaEncerraSessoesEAvisa() {
        String novoHash = encoder.encode(NOVA_SENHA);
        AlteracaoPendente pendente = pendente(TipoAlteracao.SENHA, novoHash,
                PerfilService.sha256(usuario.getSenhaHash()));
        when(alteracaoRepository.findByTokenHashParaUso(PerfilService.sha256("tok"))).thenReturn(Optional.of(pendente));

        TipoAlteracao tipo = service.confirmar("tok");

        assertThat(tipo).isEqualTo(TipoAlteracao.SENHA);
        assertThat(usuario.getSenhaHash()).isEqualTo(novoHash);
        assertThat(pendente.getConfirmadoEm()).isEqualTo(AGORA);
        verify(refreshTokenService).revogarTodos(1L);
        verify(tokenRedefinicaoRepository).invalidarPendentes(1L, AGORA);
        assertThat(emailPublicado().assunto()).isEqualTo(PerfilEmails.ASSUNTO_SENHA_ALTERADA);
    }

    @Test
    void linkDeSenhaNaoValeSeASenhaMudouDepoisDoPedido() {
        AlteracaoPendente pendente = pendente(TipoAlteracao.SENHA, encoder.encode(NOVA_SENHA),
                PerfilService.sha256("hash-de-uma-senha-anterior"));
        when(alteracaoRepository.findByTokenHashParaUso(PerfilService.sha256("tok"))).thenReturn(Optional.of(pendente));

        assertThatThrownBy(() -> service.confirmar("tok")).hasMessage(PerfilService.MSG_LINK_INVALIDO);
        verify(refreshTokenService, never()).revogarTodos(anyLong());
    }

    @Test
    void confirmarTrocaDeEmailGravaEAvisaOEnderecoAntigo() {
        AlteracaoPendente pendente = pendente(TipoAlteracao.EMAIL, "maria.nova@exemplo.com", "maria@exemplo.com");
        when(alteracaoRepository.findByTokenHashParaUso(PerfilService.sha256("tok"))).thenReturn(Optional.of(pendente));

        service.confirmar("tok");

        assertThat(usuario.getEmail()).isEqualTo("maria.nova@exemplo.com");
        verify(refreshTokenService, never()).revogarTodos(anyLong());
        verify(tokenRedefinicaoRepository).invalidarPendentes(1L, AGORA);
        EmailMessage aviso = emailPublicado();
        assertThat(aviso.para()).isEqualTo("maria@exemplo.com");
        assertThat(aviso.assunto()).isEqualTo(PerfilEmails.ASSUNTO_EMAIL_ALTERADO);
        assertThat(aviso.texto()).contains("ma***@exemplo.com").doesNotContain("maria.nova@");
    }

    @Test
    void linkExpiradoUsadoOuInexistente() {
        AlteracaoPendente expirado = pendente(TipoAlteracao.EMAIL, "x@x.com", "maria@exemplo.com");
        ReflectionTestUtils.setField(expirado, "expiraEm", AGORA.minusSeconds(1));
        AlteracaoPendente usado = pendente(TipoAlteracao.EMAIL, "x@x.com", "maria@exemplo.com");
        ReflectionTestUtils.setField(usado, "confirmadoEm", AGORA.minusSeconds(60));
        when(alteracaoRepository.findByTokenHash(PerfilService.sha256("expirado"))).thenReturn(Optional.of(expirado));
        when(alteracaoRepository.findByTokenHash(PerfilService.sha256("usado"))).thenReturn(Optional.of(usado));

        assertThatThrownBy(() -> service.validar("expirado")).hasMessage(PerfilService.MSG_LINK_INVALIDO);
        assertThatThrownBy(() -> service.validar("usado")).hasMessage(PerfilService.MSG_LINK_INVALIDO);
        assertThatThrownBy(() -> service.validar("nao-existe")).hasMessage(PerfilService.MSG_LINK_INVALIDO);
        assertThatThrownBy(() -> service.validar(" ")).hasMessage(PerfilService.MSG_LINK_INVALIDO);
    }

    @Test
    void validarDizOQueOLinkConfirmaSemAlterarNada() {
        AlteracaoPendente pendente = pendente(TipoAlteracao.EMAIL, "maria.nova@exemplo.com", "maria@exemplo.com");
        when(alteracaoRepository.findByTokenHash(PerfilService.sha256("tok"))).thenReturn(Optional.of(pendente));

        ConfirmacaoInfoResponse info = service.validar("tok");

        assertThat(info.tipo()).isEqualTo(TipoAlteracao.EMAIL);
        assertThat(info.novoEmail()).isEqualTo("maria.nova@exemplo.com");
        assertThat(usuario.getEmail()).isEqualTo("maria@exemplo.com");
        assertThat(pendente.getConfirmadoEm()).isNull();
    }

    @Test
    void cancelarAnulaOPedidoEmAberto() {
        service.cancelar(1L, TipoAlteracao.EMAIL);

        verify(alteracaoRepository).invalidarAbertas(eq(1L), eq(TipoAlteracao.EMAIL), eq(AGORA));
    }

    @Test
    void mascaraDoEmail() {
        assertThat(PerfilEmails.mascarar("maria.souza@empresa.com.br")).isEqualTo("ma***@empresa.com.br");
        assertThat(PerfilEmails.mascarar("a@b.com")).isEqualTo("a***@b.com");
    }

    // ---------- apoio ----------

    private AlteracaoPendente pendenteSalva() {
        ArgumentCaptor<AlteracaoPendente> captor = ArgumentCaptor.forClass(AlteracaoPendente.class);
        verify(alteracaoRepository).save(captor.capture());
        return captor.getValue();
    }

    private EmailMessage emailPublicado() {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(captor.capture());
        return ((EmailPendente) captor.getValue()).email();
    }

    private static String tokenDoLink(String texto) {
        String marcador = "https://app.exemplo.com/confirmar-alteracao#token=";
        int inicio = texto.indexOf(marcador);
        assertThat(inicio).isGreaterThanOrEqualTo(0);
        return texto.substring(inicio + marcador.length()).split("\\s")[0];
    }

    private AlteracaoPendente pendente(TipoAlteracao tipo, String valorNovo, String referencia) {
        return new AlteracaoPendente(1L, tipo, valorNovo, referencia, "hash", AGORA.minusSeconds(60),
                AGORA.plus(Duration.ofMinutes(29)));
    }

    private Usuario usuario() {
        Usuario u = BeanUtils.instantiateClass(Usuario.class);
        ReflectionTestUtils.setField(u, "id", 1L);
        ReflectionTestUtils.setField(u, "tenantId", 10L);
        ReflectionTestUtils.setField(u, "login", "maria.souza");
        ReflectionTestUtils.setField(u, "senhaHash", encoder.encode(SENHA_ATUAL));
        ReflectionTestUtils.setField(u, "ativo", true);
        ReflectionTestUtils.setField(u, "primeiroAcessoConcluido", true);
        ReflectionTestUtils.setField(u, "nomeCompleto", "Maria Souza");
        ReflectionTestUtils.setField(u, "email", "maria@exemplo.com");
        ReflectionTestUtils.setField(u, "telefone", "85999991234");
        return u;
    }

    private static Tenant tenant() {
        Tenant t = BeanUtils.instantiateClass(Tenant.class);
        ReflectionTestUtils.setField(t, "id", 10L);
        ReflectionTestUtils.setField(t, "nomeEmpresa", "ILLUM Contabilidade");
        ReflectionTestUtils.setField(t, "ativo", true);
        return t;
    }
}
