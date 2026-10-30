package br.com.manageraccounting.modules.auth;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.manageraccounting.core.config.AppWebProperties;
import br.com.manageraccounting.core.exception.BusinessException;
import br.com.manageraccounting.modules.tenant.Tenant;
import br.com.manageraccounting.modules.tenant.TenantRepository;
import br.com.manageraccounting.modules.usuario.Usuario;
import br.com.manageraccounting.modules.usuario.UsuarioRepository;

/**
 * "Esqueceu sua senha?": pedido do link por e-mail, validação do link e troca da senha.
 *
 * <p>O pedido nunca revela se o usuário existe: a resposta é sempre a mesma e o e-mail sai em segundo plano.
 * Só recebe o link quem está ativo, com o tenant ativo e com o primeiro acesso concluído (é no primeiro acesso
 * que o e-mail é cadastrado). Quem ainda não fez o primeiro acesso usa a senha temporária do escritório.
 */
@Service
public class RecuperacaoSenhaService {

    static final String MENSAGEM_LINK_INVALIDO =
            "Este link de redefinição é inválido ou expirou. Peça um novo em \"Esqueceu sua senha?\".";

    static final String CAMINHO_REDEFINICAO = "/redefinir-senha";
    static final String CAMINHO_LOGIN = "/login";

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Duration JANELA_LIMITE = Duration.ofHours(1);

    private final UsuarioRepository usuarioRepository;
    private final TenantRepository tenantRepository;
    private final TokenRedefinicaoSenhaRepository tokenRepository;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final RecuperacaoSenhaProperties properties;
    private final AppWebProperties webProperties;
    private final Clock clock;

    public RecuperacaoSenhaService(UsuarioRepository usuarioRepository, TenantRepository tenantRepository,
            TokenRedefinicaoSenhaRepository tokenRepository, RefreshTokenService refreshTokenService,
            PasswordEncoder passwordEncoder, ApplicationEventPublisher eventPublisher,
            RecuperacaoSenhaProperties properties, AppWebProperties webProperties, Clock clock) {
        this.usuarioRepository = usuarioRepository;
        this.tenantRepository = tenantRepository;
        this.tokenRepository = tokenRepository;
        this.refreshTokenService = refreshTokenService;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
        this.properties = properties;
        this.webProperties = webProperties;
        this.clock = clock;
    }

    public Duration getTokenTtl() {
        return properties.tokenTtl();
    }

    /**
     * Gera um link de redefinição e agenda o e-mail. Em qualquer caso em que o link não deva ser enviado
     * (usuário inexistente, inativo, sem primeiro acesso, limite de envios), termina em silêncio.
     */
    @Transactional
    public void solicitar(String login) {
        Instant agora = clock.instant();

        Usuario usuario = usuarioRepository.findByLogin(login.trim())
                .filter(Usuario::podeRecuperarSenhaPorEmail)
                .orElse(null);
        if (usuario == null) {
            return;
        }
        Tenant tenant = tenantAtivo(usuario.getTenantId()).orElse(null);
        if (tenant == null || atingiuLimiteDeEnvios(usuario.getId(), agora)) {
            return;
        }

        // Só o link mais recente vale
        tokenRepository.invalidarPendentes(usuario.getId(), agora);

        String token = gerarToken();
        tokenRepository.save(new TokenRedefinicaoSenha(
                usuario.getId(), RefreshTokenService.hash(token), agora, agora.plus(properties.tokenTtl())));

        // O token vai depois do "#": o navegador não envia essa parte ao servidor nem a outros sites (Referer)
        String link = webProperties.baseUrlSemBarra() + CAMINHO_REDEFINICAO + "#token=" + token;
        eventPublisher.publishEvent(new EmailPendente(RecuperacaoSenhaEmails.link(
                usuario.getEmail(), usuario.getNomeCompleto(), tenant.getNomeEmpresa(), link,
                properties.tokenTtl().toMinutes())));
    }

    /** Confere o link antes de mostrar o formulário de nova senha. */
    @Transactional(readOnly = true)
    public void validar(String token) {
        buscarUsuarioDoToken(token, false);
    }

    /**
     * Troca a senha, consome o link (uso único), anula os outros links pendentes, tira o bloqueio de login e
     * derruba todas as sessões abertas do usuário.
     */
    @Transactional
    public void redefinir(String token, String novaSenha) {
        Instant agora = clock.instant();
        TokenComUsuario encontrado = buscarUsuarioDoToken(token, true);
        PasswordPolicy.validar(novaSenha);

        Usuario usuario = encontrado.usuario();
        usuario.redefinirSenha(passwordEncoder.encode(novaSenha));
        encontrado.token().marcarComoUtilizado(agora);
        tokenRepository.invalidarPendentes(usuario.getId(), agora);
        refreshTokenService.revogarTodos(usuario.getId());
        usuario.encerrarSessoes(agora);

        String nomeEmpresa = tenantAtivo(usuario.getTenantId()).map(Tenant::getNomeEmpresa).orElse("");
        eventPublisher.publishEvent(new EmailPendente(RecuperacaoSenhaEmails.confirmacao(
                usuario.getEmail(), usuario.getNomeCompleto(), nomeEmpresa,
                webProperties.baseUrlSemBarra() + CAMINHO_LOGIN)));
    }

    /** Limpeza diária dos links expirados há mais de um dia. */
    @Scheduled(cron = "0 15 3 * * *")
    @Transactional
    public void limparExpirados() {
        tokenRepository.apagarExpiradosAte(clock.instant().minus(Duration.ofDays(1)));
    }

    private TokenComUsuario buscarUsuarioDoToken(String token, boolean paraUso) {
        if (token == null || token.isBlank()) {
            throw new BusinessException(MENSAGEM_LINK_INVALIDO);
        }
        Instant agora = clock.instant();
        String hash = RefreshTokenService.hash(token.trim());

        TokenRedefinicaoSenha encontrado = (paraUso
                ? tokenRepository.findByTokenHashParaUso(hash)
                : tokenRepository.findByTokenHash(hash))
                .filter(t -> t.estaValido(agora))
                .orElseThrow(() -> new BusinessException(MENSAGEM_LINK_INVALIDO));

        Usuario usuario = usuarioRepository.findAtivoById(encontrado.getUsuarioId())
                .filter(Usuario::podeRecuperarSenhaPorEmail)
                .orElseThrow(() -> new BusinessException(MENSAGEM_LINK_INVALIDO));
        return new TokenComUsuario(encontrado, usuario);
    }

    /** No mínimo 2 minutos entre e-mails e no máximo 5 por hora (valores padrão) para o mesmo usuário. */
    private boolean atingiuLimiteDeEnvios(Long usuarioId, Instant agora) {
        return tokenRepository.existsByUsuarioIdAndCriadoEmAfter(usuarioId, agora.minus(properties.intervaloMinimo()))
                || tokenRepository.countByUsuarioIdAndCriadoEmAfter(usuarioId, agora.minus(JANELA_LIMITE))
                        >= properties.maxEnviosPorHora();
    }

    private Optional<Tenant> tenantAtivo(Long tenantId) {
        return tenantRepository.findById(tenantId).filter(Tenant::isAtivo);
    }

    private static String gerarToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private record TokenComUsuario(TokenRedefinicaoSenha token, Usuario usuario) {
    }
}
