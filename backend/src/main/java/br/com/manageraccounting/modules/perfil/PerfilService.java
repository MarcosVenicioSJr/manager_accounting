package br.com.manageraccounting.modules.perfil;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.manageraccounting.core.config.AppWebProperties;
import br.com.manageraccounting.core.exception.BusinessException;
import br.com.manageraccounting.core.exception.UnauthorizedException;
import br.com.manageraccounting.modules.auth.AuthService;
import br.com.manageraccounting.modules.auth.EmailPendente;
import br.com.manageraccounting.modules.auth.PasswordPolicy;
import br.com.manageraccounting.modules.auth.RefreshTokenService;
import br.com.manageraccounting.modules.auth.TokenRedefinicaoSenhaRepository;
import br.com.manageraccounting.modules.perfil.dto.AtualizarPerfilRequest;
import br.com.manageraccounting.modules.perfil.dto.ConfirmacaoInfoResponse;
import br.com.manageraccounting.modules.perfil.dto.PerfilResponse;
import br.com.manageraccounting.modules.perfil.dto.PerfilResponse.TrocaPendente;
import br.com.manageraccounting.modules.tenant.Tenant;
import br.com.manageraccounting.modules.tenant.TenantRepository;
import br.com.manageraccounting.modules.usuario.Telefones;
import br.com.manageraccounting.modules.usuario.Usuario;
import br.com.manageraccounting.modules.usuario.UsuarioRepository;

/**
 * Meu perfil do contador logado.
 * <ul>
 *   <li>Nome e telefone mudam na hora.</li>
 *   <li>Senha e e-mail passam por duas etapas: o pedido (com a senha atual) grava uma alteração pendente e envia
 *   um link; a troca só acontece quando a pessoa abre o link e confirma.</li>
 *   <li>Troca de senha: o link vai para o e-mail atual. Depois de confirmada, todas as sessões são encerradas.</li>
 *   <li>Troca de e-mail: o link vai para o novo endereço (prova que a pessoa tem acesso a ele) e, depois de
 *   confirmada, o endereço antigo recebe um aviso.</li>
 * </ul>
 * Senha atual errada conta como tentativa errada de login (5 erros bloqueiam por 15 minutos).
 */
@Service
public class PerfilService {

    static final String CAMINHO_CONFIRMACAO = "/confirmar-alteracao";
    static final String CAMINHO_LOGIN = "/login";
    static final String MSG_LINK_INVALIDO =
            "Este link de confirmação é inválido ou expirou. Faça o pedido de novo no Meu perfil.";
    static final String MSG_SENHA_ATUAL = "Senha atual incorreta.";
    static final String MSG_BLOQUEADO =
            "Muitas tentativas com a senha errada. Aguarde alguns minutos e tente de novo.";
    static final String MSG_AGUARDE = "Você acabou de pedir um e-mail. Aguarde um pouco antes de pedir outro.";

    private static final Logger log = LoggerFactory.getLogger(PerfilService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Duration JANELA_LIMITE = Duration.ofHours(1);

    private final UsuarioRepository usuarioRepository;
    private final TenantRepository tenantRepository;
    private final AlteracaoPendenteRepository alteracaoRepository;
    private final TokenRedefinicaoSenhaRepository tokenRedefinicaoRepository;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final PerfilProperties properties;
    private final AppWebProperties webProperties;
    private final Clock clock;

    public PerfilService(UsuarioRepository usuarioRepository, TenantRepository tenantRepository,
            AlteracaoPendenteRepository alteracaoRepository, TokenRedefinicaoSenhaRepository tokenRedefinicaoRepository,
            RefreshTokenService refreshTokenService, PasswordEncoder passwordEncoder,
            ApplicationEventPublisher eventPublisher, PerfilProperties properties, AppWebProperties webProperties,
            Clock clock) {
        this.usuarioRepository = usuarioRepository;
        this.tenantRepository = tenantRepository;
        this.alteracaoRepository = alteracaoRepository;
        this.tokenRedefinicaoRepository = tokenRedefinicaoRepository;
        this.refreshTokenService = refreshTokenService;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
        this.properties = properties;
        this.webProperties = webProperties;
        this.clock = clock;
    }

    // ---------- leitura e dados simples ----------

    @Transactional(readOnly = true)
    public PerfilResponse obter(Long usuarioId) {
        return montar(usuarioLogado(usuarioId));
    }

    @Transactional
    public PerfilResponse atualizarDados(Long usuarioId, AtualizarPerfilRequest request) {
        Usuario usuario = usuarioLogado(usuarioId);
        String nome = request.nomeCompleto().strip();
        if (nome.isEmpty()) {
            throw new BusinessException("Informe o nome completo.");
        }
        usuario.atualizarDados(nome, Telefones.normalizar(request.telefone()));
        return montar(usuario);
    }

    // ---------- pedidos (etapa 1) ----------

    /**
     * Pede a troca de senha: confere a senha atual e a política, guarda o hash da nova senha como pendente e
     * envia o link de confirmação para o e-mail atual. A senha atual continua valendo até a confirmação.
     */
    @Transactional(noRollbackFor = SenhaAtualIncorretaException.class)
    public TrocaPendente solicitarTrocaSenha(Long usuarioId, String senhaAtual, String novaSenha) {
        Instant agora = clock.instant();
        Usuario usuario = usuarioLogado(usuarioId);
        conferirSenhaAtual(usuario, senhaAtual, agora);

        PasswordPolicy.validar(novaSenha);
        if (passwordEncoder.matches(novaSenha, usuario.getSenhaHash())) {
            throw new BusinessException("A nova senha deve ser diferente da senha atual.");
        }
        conferirLimite(usuarioId, TipoAlteracao.SENHA, agora);

        PedidoGravado pedido = registrar(usuario, TipoAlteracao.SENHA, passwordEncoder.encode(novaSenha),
                sha256(usuario.getSenhaHash()), agora);
        eventPublisher.publishEvent(new EmailPendente(PerfilEmails.confirmarSenha(
                usuario.getEmail(), usuario.getNomeCompleto(), nomeEscritorio(usuario), pedido.link(),
                properties.tokenTtl().toMinutes())));

        log.info("Troca de senha pedida no Meu perfil: usuario={}", usuarioId);
        return new TrocaPendente(usuario.getEmail(), null, pedido.pendente().getExpiraEm());
    }

    /**
     * Pede a troca de e-mail: confere a senha atual e envia o link de confirmação para o NOVO endereço.
     * O e-mail atual continua valendo até a confirmação.
     */
    @Transactional(noRollbackFor = SenhaAtualIncorretaException.class)
    public TrocaPendente solicitarTrocaEmail(Long usuarioId, String senhaAtual, String novoEmail) {
        Instant agora = clock.instant();
        Usuario usuario = usuarioLogado(usuarioId);
        conferirSenhaAtual(usuario, senhaAtual, agora);

        String email = novoEmail.strip().toLowerCase(Locale.ROOT);
        if (email.equals(usuario.getEmail())) {
            throw new BusinessException("Este já é o seu e-mail atual.");
        }
        conferirLimite(usuarioId, TipoAlteracao.EMAIL, agora);

        PedidoGravado pedido = registrar(usuario, TipoAlteracao.EMAIL, email, usuario.getEmail(), agora);
        eventPublisher.publishEvent(new EmailPendente(PerfilEmails.confirmarEmail(
                email, usuario.getNomeCompleto(), nomeEscritorio(usuario), pedido.link(),
                properties.tokenTtl().toMinutes())));

        log.info("Troca de e-mail pedida no Meu perfil: usuario={}", usuarioId);
        return new TrocaPendente(email, email, pedido.pendente().getExpiraEm());
    }

    /** Cancela o pedido em aberto (o link deixa de funcionar). Sem pedido, não muda nada. */
    @Transactional
    public void cancelar(Long usuarioId, TipoAlteracao tipo) {
        usuarioLogado(usuarioId);
        alteracaoRepository.invalidarAbertas(usuarioId, tipo, clock.instant());
    }

    // ---------- confirmação (etapa 2, pelo link do e-mail) ----------

    /** Confere o link e diz o que ele confirma, sem alterar nada (a tela pede o clique em "Confirmar"). */
    @Transactional(readOnly = true)
    public ConfirmacaoInfoResponse validar(String token) {
        PendenteComUsuario encontrado = buscar(token, false);
        AlteracaoPendente pendente = encontrado.pendente();
        return new ConfirmacaoInfoResponse(pendente.getTipo(),
                pendente.getTipo() == TipoAlteracao.EMAIL ? pendente.getValorNovo() : null);
    }

    /**
     * Aplica a alteração e consome o link (uso único).
     * <ul>
     *   <li>SENHA: troca o hash, tira o bloqueio de login, anula links de recuperação pendentes e encerra todas as
     *   sessões. Se a senha mudou por outro caminho depois do pedido, o link não vale mais.</li>
     *   <li>EMAIL: grava o novo e-mail, anula links de recuperação enviados ao endereço antigo e avisa o antigo.</li>
     * </ul>
     */
    @Transactional
    public TipoAlteracao confirmar(String token) {
        Instant agora = clock.instant();
        PendenteComUsuario encontrado = buscar(token, true);
        AlteracaoPendente pendente = encontrado.pendente();
        Usuario usuario = encontrado.usuario();
        String linkLogin = webProperties.baseUrlSemBarra() + CAMINHO_LOGIN;

        pendente.confirmar(agora);
        alteracaoRepository.invalidarAbertas(usuario.getId(), pendente.getTipo(), agora);
        tokenRedefinicaoRepository.invalidarPendentes(usuario.getId(), agora);

        if (pendente.getTipo() == TipoAlteracao.SENHA) {
            usuario.redefinirSenha(pendente.getValorNovo());
            refreshTokenService.revogarTodos(usuario.getId());
            eventPublisher.publishEvent(new EmailPendente(PerfilEmails.senhaAlterada(
                    usuario.getEmail(), usuario.getNomeCompleto(), nomeEscritorio(usuario), linkLogin)));
            log.info("Troca de senha confirmada por e-mail: usuario={}", usuario.getId());
        } else {
            String emailAntigo = usuario.getEmail();
            usuario.alterarEmail(pendente.getValorNovo());
            if (emailAntigo != null && !emailAntigo.isBlank()) {
                eventPublisher.publishEvent(new EmailPendente(PerfilEmails.emailAlterado(
                        emailAntigo, usuario.getNomeCompleto(), nomeEscritorio(usuario),
                        PerfilEmails.mascarar(pendente.getValorNovo()))));
            }
            log.info("Troca de e-mail confirmada: usuario={}", usuario.getId());
        }
        return pendente.getTipo();
    }

    /** Limpeza diária dos pedidos expirados há mais de um dia. */
    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void limparExpiradas() {
        alteracaoRepository.apagarExpiradasAte(clock.instant().minus(Duration.ofDays(1)));
    }

    // ---------- apoio ----------

    private Usuario usuarioLogado(Long usuarioId) {
        return usuarioRepository.findAtivoById(usuarioId)
                .orElseThrow(() -> new UnauthorizedException("Sessão inválida"));
    }

    private void conferirSenhaAtual(Usuario usuario, String senhaAtual, Instant agora) {
        if (usuario.estaBloqueado(agora)) {
            throw new BusinessException(MSG_BLOQUEADO);
        }
        if (senhaAtual == null || PasswordPolicy.excedeLimiteDoBcrypt(senhaAtual)
                || !passwordEncoder.matches(senhaAtual, usuario.getSenhaHash())) {
            usuario.registrarFalhaDeLogin(agora, AuthService.MAX_TENTATIVAS, AuthService.DURACAO_BLOQUEIO);
            throw new SenhaAtualIncorretaException(
                    usuario.estaBloqueado(agora) ? MSG_BLOQUEADO : MSG_SENHA_ATUAL);
        }
        usuario.registrarLoginComSucesso();
    }

    private void conferirLimite(Long usuarioId, TipoAlteracao tipo, Instant agora) {
        boolean muitoCedo = alteracaoRepository.existsByUsuarioIdAndTipoAndCriadoEmAfter(
                usuarioId, tipo, agora.minus(properties.intervaloMinimo()));
        boolean passouDoLimite = alteracaoRepository.countByUsuarioIdAndTipoAndCriadoEmAfter(
                usuarioId, tipo, agora.minus(JANELA_LIMITE)) >= properties.maxEnviosPorHora();
        if (muitoCedo || passouDoLimite) {
            throw new BusinessException(MSG_AGUARDE);
        }
    }

    /** Anula o pedido anterior do mesmo tipo (só o link mais recente vale) e grava o novo. */
    private PedidoGravado registrar(Usuario usuario, TipoAlteracao tipo, String valorNovo, String referencia,
            Instant agora) {
        alteracaoRepository.invalidarAbertas(usuario.getId(), tipo, agora);
        String token = gerarToken();
        AlteracaoPendente pendente = new AlteracaoPendente(usuario.getId(), tipo, valorNovo, referencia,
                sha256(token), agora, agora.plus(properties.tokenTtl()));
        alteracaoRepository.save(pendente);
        // O token vai depois do "#": não é enviado ao servidor do front nem a outros sites (Referer)
        String link = webProperties.baseUrlSemBarra() + CAMINHO_CONFIRMACAO + "#token=" + token;
        return new PedidoGravado(pendente, link);
    }

    private PendenteComUsuario buscar(String token, boolean paraUso) {
        if (token == null || token.isBlank()) {
            throw new BusinessException(MSG_LINK_INVALIDO);
        }
        Instant agora = clock.instant();
        String hash = sha256(token.trim());

        AlteracaoPendente pendente = (paraUso
                ? alteracaoRepository.findByTokenHashParaUso(hash)
                : alteracaoRepository.findByTokenHash(hash))
                .filter(p -> p.estaValida(agora))
                .orElseThrow(() -> new BusinessException(MSG_LINK_INVALIDO));

        Usuario usuario = usuarioRepository.findAtivoById(pendente.getUsuarioId())
                .orElseThrow(() -> new BusinessException(MSG_LINK_INVALIDO));

        // Troca de senha: se a senha mudou por outro caminho (ex.: "Esqueceu sua senha?") depois do pedido,
        // este link não vale mais
        if (pendente.getTipo() == TipoAlteracao.SENHA
                && !sha256(usuario.getSenhaHash()).equals(pendente.getReferencia())) {
            throw new BusinessException(MSG_LINK_INVALIDO);
        }
        return new PendenteComUsuario(pendente, usuario);
    }

    private PerfilResponse montar(Usuario usuario) {
        List<AlteracaoPendente> abertas = alteracaoRepository.findAbertas(usuario.getId(), clock.instant());
        TrocaPendente senha = null;
        TrocaPendente email = null;
        for (AlteracaoPendente a : abertas) {
            if (a.getTipo() == TipoAlteracao.SENHA && senha == null) {
                senha = new TrocaPendente(usuario.getEmail(), null, a.getExpiraEm());
            } else if (a.getTipo() == TipoAlteracao.EMAIL && email == null) {
                email = new TrocaPendente(a.getValorNovo(), a.getValorNovo(), a.getExpiraEm());
            }
        }
        return new PerfilResponse(usuario.getLogin(), usuario.getNomeCompleto(), usuario.getEmail(),
                usuario.getTelefone(), nomeEscritorio(usuario), senha, email);
    }

    private String nomeEscritorio(Usuario usuario) {
        return tenantRepository.findById(usuario.getTenantId()).map(Tenant::getNomeEmpresa).orElse("");
    }

    private static String gerarToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** SHA-256 em hexadecimal (64 caracteres). */
    static String sha256(String valor) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponível", ex);
        }
    }

    private record PendenteComUsuario(AlteracaoPendente pendente, Usuario usuario) {
    }

    private record PedidoGravado(AlteracaoPendente pendente, String link) {
    }
}
