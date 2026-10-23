package br.com.manageraccounting.modules.equipe;

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
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.manageraccounting.core.config.AppWebProperties;
import br.com.manageraccounting.core.exception.BusinessException;
import br.com.manageraccounting.core.exception.ConflictException;
import br.com.manageraccounting.core.exception.NotFoundException;
import br.com.manageraccounting.core.exception.UnauthorizedException;
import br.com.manageraccounting.modules.auth.EmailPendente;
import br.com.manageraccounting.modules.auth.PasswordPolicy;
import br.com.manageraccounting.modules.auth.RefreshTokenService;
import br.com.manageraccounting.modules.equipe.dto.ConvidarUsuarioRequest;
import br.com.manageraccounting.modules.equipe.dto.ConviteInfoResponse;
import br.com.manageraccounting.modules.equipe.dto.UsuarioEquipeResponse;
import br.com.manageraccounting.modules.equipe.dto.UsuarioEquipeResponse.Situacao;
import br.com.manageraccounting.modules.tenant.Tenant;
import br.com.manageraccounting.modules.tenant.TenantRepository;
import br.com.manageraccounting.modules.usuario.PerfilAcesso;
import br.com.manageraccounting.modules.usuario.Telefones;
import br.com.manageraccounting.modules.usuario.Usuario;
import br.com.manageraccounting.modules.usuario.UsuarioRepository;

/**
 * Usuários do escritório, geridos pelo administrador (perfil ADMIN; a rota já exige {@code ROLE_ADMIN}).
 * <ul>
 *   <li>O escritório é sempre o do administrador logado: usuário de outro escritório responde "não encontrado".</li>
 *   <li>Convite: o usuário é criado na hora (login reservado, senha aleatória que ninguém conhece) e recebe por
 *   e-mail um link para criar a senha. Ao aceitar, o primeiro acesso fica concluído.</li>
 *   <li>O escritório nunca fica sem administrador em atividade, e ninguém muda o próprio perfil nem se desativa.</li>
 *   <li>Desativar encerra as sessões na hora (o filtro JWT confere o usuário no banco a cada requisição).</li>
 * </ul>
 */
@Service
public class EquipeService {

    static final String CAMINHO_CONVITE = "/aceitar-convite";
    static final String CODIGO_LOGIN_EM_USO = "LOGIN_EM_USO";
    static final String MSG_LOGIN_EM_USO = "Este usuário de acesso já está em uso. Escolha outro.";
    static final String MSG_NAO_ENCONTRADO = "Usuário não encontrado.";
    static final String MSG_CONVITE_INVALIDO =
            "Este convite é inválido ou expirou. Peça ao administrador do escritório para reenviar.";
    static final String MSG_ULTIMO_ADMIN =
            "O escritório precisa de pelo menos um administrador ativo. Torne outro usuário administrador antes.";
    static final String MSG_AGUARDE = "O convite acabou de ser enviado. Aguarde um pouco antes de reenviar.";

    /** Letras minúsculas, números, ponto, hífen e sublinhado; começa com letra ou número; 3 a 60 caracteres. */
    static final Pattern LOGIN = Pattern.compile("[a-z0-9][a-z0-9._-]{2,59}");

    private static final Logger log = LoggerFactory.getLogger(EquipeService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final TenantRepository tenantRepository;
    private final ConviteUsuarioRepository conviteRepository;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final EquipeProperties properties;
    private final AppWebProperties webProperties;
    private final Clock clock;

    public EquipeService(UsuarioRepository usuarioRepository, TenantRepository tenantRepository,
            ConviteUsuarioRepository conviteRepository, RefreshTokenService refreshTokenService,
            PasswordEncoder passwordEncoder, ApplicationEventPublisher eventPublisher, EquipeProperties properties,
            AppWebProperties webProperties, Clock clock) {
        this.usuarioRepository = usuarioRepository;
        this.tenantRepository = tenantRepository;
        this.conviteRepository = conviteRepository;
        this.refreshTokenService = refreshTokenService;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
        this.properties = properties;
        this.webProperties = webProperties;
        this.clock = clock;
    }

    // ---------- lista ----------

    @Transactional(readOnly = true)
    public List<UsuarioEquipeResponse> listar(Long tenantId, Long adminId) {
        List<Usuario> usuarios = usuarioRepository.findByTenantIdOrderByAtivoDescNomeCompletoAscLoginAsc(tenantId);
        Map<Long, Instant> convites = usuarios.isEmpty()
                ? Map.of()
                : conviteRepository.findAbertos(usuarios.stream().map(Usuario::getId).toList(), clock.instant())
                        .stream()
                        .collect(Collectors.toMap(ConviteUsuario::getUsuarioId, ConviteUsuario::getExpiraEm,
                                (a, b) -> a.isAfter(b) ? a : b));
        return usuarios.stream().map(u -> toResponse(u, convites.get(u.getId()), adminId)).toList();
    }

    // ---------- convite ----------

    /** Cria o usuário (pendente) e envia o convite. Login único na plataforma inteira. */
    @Transactional
    public UsuarioEquipeResponse convidar(Long tenantId, Long adminId, ConvidarUsuarioRequest request) {
        Instant agora = clock.instant();
        String login = request.login().strip().toLowerCase(Locale.ROOT);
        if (!LOGIN.matcher(login).matches()) {
            throw new BusinessException(
                    "Usuário de acesso inválido. Use de 3 a 60 letras minúsculas, números, ponto, hífen ou sublinhado.");
        }
        String nome = request.nomeCompleto().strip();
        if (nome.isEmpty()) {
            throw new BusinessException("Informe o nome completo.");
        }
        if (usuarioRepository.existsByLogin(login)) {
            throw new ConflictException(CODIGO_LOGIN_EM_USO, MSG_LOGIN_EM_USO);
        }

        Usuario novo = Usuario.convidado(tenantId, login, passwordEncoder.encode(gerarToken()), nome,
                request.email().strip().toLowerCase(Locale.ROOT), request.perfil(), agora);
        try {
            usuarioRepository.saveAndFlush(novo);
        } catch (DataIntegrityViolationException ex) {
            // Dois convites com o mesmo login ao mesmo tempo: o índice único segura o segundo
            throw new ConflictException(CODIGO_LOGIN_EM_USO, MSG_LOGIN_EM_USO);
        }

        ConviteUsuario convite = enviarConvite(novo, admin(adminId, tenantId), agora);
        log.info("Usuário convidado: tenant={} usuario={} perfil={} por={}", tenantId, novo.getId(),
                novo.getPerfil(), adminId);
        return toResponse(novo, convite.getExpiraEm(), adminId);
    }

    /** Novo link para quem ainda não criou a senha. O link anterior deixa de valer. */
    @Transactional
    public UsuarioEquipeResponse reenviarConvite(Long tenantId, Long adminId, Long usuarioId) {
        Instant agora = clock.instant();
        Usuario usuario = buscar(tenantId, usuarioId);
        if (usuario.isPrimeiroAcessoConcluido()) {
            throw new BusinessException("Este usuário já criou a senha. Não há convite para reenviar.");
        }
        if (!usuario.isAtivo()) {
            throw new BusinessException("Reative o usuário antes de reenviar o convite.");
        }
        if (usuario.getEmail() == null || usuario.getEmail().isBlank()) {
            throw new BusinessException("Este usuário não tem e-mail cadastrado para receber o convite.");
        }
        if (conviteRepository.existsByUsuarioIdAndCriadoEmAfter(usuarioId, agora.minus(properties.intervaloMinimo()))) {
            throw new BusinessException(MSG_AGUARDE);
        }
        ConviteUsuario convite = enviarConvite(usuario, admin(adminId, tenantId), agora);
        log.info("Convite reenviado: tenant={} usuario={} por={}", tenantId, usuarioId, adminId);
        return toResponse(usuario, convite.getExpiraEm(), adminId);
    }

    /**
     * Cancela o convite de quem ainda não criou a senha: o usuário é apagado e o login fica livre de novo
     * (para corrigir um e-mail ou login digitado errado, por exemplo).
     */
    @Transactional
    public void cancelarConvite(Long tenantId, Long adminId, Long usuarioId) {
        Usuario usuario = buscar(tenantId, usuarioId);
        if (usuario.isPrimeiroAcessoConcluido()) {
            throw new BusinessException("Este usuário já criou a senha. Para tirar o acesso, desative-o.");
        }
        refreshTokenService.apagarTodos(usuarioId);
        usuarioRepository.delete(usuario); // os convites saem junto (ON DELETE CASCADE)
        log.info("Convite cancelado e usuário apagado: tenant={} usuario={} por={}", tenantId, usuarioId, adminId);
    }

    // ---------- perfil e situação ----------

    @Transactional
    public UsuarioEquipeResponse alterarPerfil(Long tenantId, Long adminId, Long usuarioId, PerfilAcesso perfil) {
        Usuario usuario = buscar(tenantId, usuarioId);
        if (usuario.getId().equals(adminId)) {
            throw new BusinessException("Você não pode mudar o seu próprio perfil. Peça a outro administrador.");
        }
        if (usuario.getPerfil() != perfil) {
            if (perfil != PerfilAcesso.ADMIN) {
                garantirOutroAdmin(tenantId, usuario);
            }
            usuario.alterarPerfil(perfil);
            log.info("Perfil alterado: tenant={} usuario={} perfil={} por={}", tenantId, usuarioId, perfil, adminId);
        }
        return toResponse(usuario, null, adminId);
    }

    /** Tira o acesso na hora: encerra as sessões e anula convites em aberto. Repetir não muda nada. */
    @Transactional
    public void desativar(Long tenantId, Long adminId, Long usuarioId) {
        Usuario usuario = buscar(tenantId, usuarioId);
        if (usuario.getId().equals(adminId)) {
            throw new BusinessException("Você não pode desativar o seu próprio acesso.");
        }
        if (!usuario.isAtivo()) {
            return;
        }
        garantirOutroAdmin(tenantId, usuario);
        Instant agora = clock.instant();
        usuario.desativar();
        refreshTokenService.revogarTodos(usuarioId);
        conviteRepository.invalidarAbertos(usuarioId, agora);
        log.info("Usuário desativado: tenant={} usuario={} por={}", tenantId, usuarioId, adminId);
    }

    /** Devolve o acesso. Quem ainda não criou a senha precisa de um novo convite. */
    @Transactional
    public void reativar(Long tenantId, Long adminId, Long usuarioId) {
        Usuario usuario = buscar(tenantId, usuarioId);
        if (!usuario.isAtivo()) {
            usuario.reativar();
            log.info("Usuário reativado: tenant={} usuario={} por={}", tenantId, usuarioId, adminId);
        }
    }

    // ---------- aceite do convite (público, pelo link do e-mail) ----------

    @Transactional(readOnly = true)
    public ConviteInfoResponse validarConvite(String token) {
        ConviteComUsuario encontrado = buscarConvite(token, false);
        Usuario usuario = encontrado.usuario();
        return new ConviteInfoResponse(usuario.getNomeCompleto(), usuario.getLogin(), usuario.getEmail(),
                encontrado.tenant().getNomeEmpresa());
    }

    /** Cria a senha, grava nome e telefone e conclui o primeiro acesso. O link é de uso único. */
    @Transactional
    public String aceitarConvite(String token, String nomeCompleto, String telefone, String novaSenha) {
        Instant agora = clock.instant();
        ConviteComUsuario encontrado = buscarConvite(token, true);
        PasswordPolicy.validar(novaSenha);
        String nome = nomeCompleto == null ? "" : nomeCompleto.strip();
        if (nome.isEmpty()) {
            throw new BusinessException("Informe o nome completo.");
        }

        Usuario usuario = encontrado.usuario();
        usuario.concluirPrimeiroAcesso(passwordEncoder.encode(novaSenha), nome, usuario.getEmail(),
                Telefones.normalizar(telefone));
        encontrado.convite().aceitar(agora);
        conviteRepository.invalidarAbertos(usuario.getId(), agora);
        log.info("Convite aceito: tenant={} usuario={}", usuario.getTenantId(), usuario.getId());
        return usuario.getLogin();
    }

    /** Limpeza diária dos convites não aceitos expirados há mais de 30 dias (o usuário pendente continua). */
    @Scheduled(cron = "0 45 3 * * *")
    @Transactional
    public void limparExpirados() {
        conviteRepository.apagarExpiradosAte(clock.instant().minus(Duration.ofDays(30)));
    }

    // ---------- apoio ----------

    private ConviteUsuario enviarConvite(Usuario usuario, Usuario admin, Instant agora) {
        conviteRepository.invalidarAbertos(usuario.getId(), agora);
        String token = gerarToken();
        ConviteUsuario convite = conviteRepository.save(new ConviteUsuario(usuario.getId(), sha256(token),
                admin.getId(), agora, agora.plus(properties.validade())));

        // O token vai depois do "#": não é enviado ao servidor do front nem a outros sites (Referer)
        String link = webProperties.baseUrlSemBarra() + CAMINHO_CONVITE + "#token=" + token;
        String quemConvidou = admin.getNomeCompleto() == null ? admin.getLogin() : admin.getNomeCompleto();
        eventPublisher.publishEvent(new EmailPendente(EquipeEmails.convite(usuario.getEmail(),
                usuario.getNomeCompleto(), quemConvidou, nomeEscritorio(usuario.getTenantId()), usuario.getLogin(),
                link, properties.validade().toHours())));
        return convite;
    }

    /** Impede que o escritório fique sem administrador em atividade. */
    private void garantirOutroAdmin(Long tenantId, Usuario alvo) {
        if (alvo.isAdminEmAtividade() && usuarioRepository.contarAdminsEmAtividade(tenantId) <= 1) {
            throw new BusinessException(MSG_ULTIMO_ADMIN);
        }
    }

    private Usuario buscar(Long tenantId, Long usuarioId) {
        return usuarioRepository.findByIdAndTenantId(usuarioId, tenantId)
                .orElseThrow(() -> new NotFoundException(MSG_NAO_ENCONTRADO));
    }

    private Usuario admin(Long adminId, Long tenantId) {
        return usuarioRepository.findByIdAndTenantId(adminId, tenantId)
                .orElseThrow(() -> new UnauthorizedException("Sessão inválida"));
    }

    private ConviteComUsuario buscarConvite(String token, boolean paraUso) {
        if (token == null || token.isBlank()) {
            throw new BusinessException(MSG_CONVITE_INVALIDO);
        }
        Instant agora = clock.instant();
        String hash = sha256(token.trim());
        ConviteUsuario convite = (paraUso
                ? conviteRepository.findByTokenHashParaUso(hash)
                : conviteRepository.findByTokenHash(hash))
                .filter(c -> c.estaValido(agora))
                .orElseThrow(() -> new BusinessException(MSG_CONVITE_INVALIDO));
        Usuario usuario = usuarioRepository.findAtivoById(convite.getUsuarioId())
                .filter(u -> !u.isPrimeiroAcessoConcluido())
                .orElseThrow(() -> new BusinessException(MSG_CONVITE_INVALIDO));
        Tenant tenant = tenantRepository.findById(usuario.getTenantId())
                .orElseThrow(() -> new BusinessException(MSG_CONVITE_INVALIDO));
        return new ConviteComUsuario(convite, usuario, tenant);
    }

    private UsuarioEquipeResponse toResponse(Usuario u, Instant conviteExpiraEm, Long adminId) {
        Situacao situacao = !u.isAtivo() ? Situacao.INATIVO
                : !u.isPrimeiroAcessoConcluido() ? Situacao.PENDENTE
                : Situacao.ATIVO;
        return new UsuarioEquipeResponse(u.getId(), u.getLogin(), u.getNomeCompleto(), u.getEmail(), u.getPerfil(),
                situacao, situacao == Situacao.PENDENTE ? conviteExpiraEm : null, u.getDataInclusao(),
                u.getId().equals(adminId));
    }

    private String nomeEscritorio(Long tenantId) {
        return tenantRepository.findById(tenantId).map(Tenant::getNomeEmpresa).orElse("");
    }

    static String sha256(String valor) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponível", ex);
        }
    }

    private static String gerarToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private record ConviteComUsuario(ConviteUsuario convite, Usuario usuario, Tenant tenant) {
    }
}
