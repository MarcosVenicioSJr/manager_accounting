package br.com.manageraccounting.modules.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.manageraccounting.core.exception.BusinessException;
import br.com.manageraccounting.core.exception.UnauthorizedException;
import br.com.manageraccounting.core.security.JwtService;
import br.com.manageraccounting.core.security.JwtService.AccessToken;
import br.com.manageraccounting.modules.auth.dto.AuthResponse;
import br.com.manageraccounting.modules.auth.dto.PrimeiroAcessoRequest;
import br.com.manageraccounting.modules.tenant.Tenant;
import br.com.manageraccounting.modules.tenant.TenantRepository;
import br.com.manageraccounting.modules.usuario.Usuario;
import br.com.manageraccounting.modules.usuario.UsuarioRepository;

/**
 * Regras de autenticação: login (com bloqueio após tentativas erradas), refresh, logout e primeiro acesso.
 */
@Service
public class AuthService {

    static final int MAX_TENTATIVAS = 5;
    static final Duration DURACAO_BLOQUEIO = Duration.ofMinutes(15);

    /** Mesma mensagem para login inexistente, senha errada, conta inativa ou bloqueada (não revela qual foi). */
    static final String MENSAGEM_LOGIN_INVALIDO = "Login ou senha inválidos, ou acesso temporariamente indisponível.";
    static final String MENSAGEM_SESSAO_INVALIDA = "Sessão expirada. Faça login novamente.";

    private static final String SENHA_FALSA = "senha-falsa";

    private final UsuarioRepository usuarioRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final Clock clock;

    /** Hash usado para gastar o mesmo tempo quando o login não existe (evita descobrir logins pelo tempo). */
    private final String hashFalso;

    public AuthService(UsuarioRepository usuarioRepository, TenantRepository tenantRepository,
            PasswordEncoder passwordEncoder, JwtService jwtService, RefreshTokenService refreshTokenService,
            Clock clock) {
        this.usuarioRepository = usuarioRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.clock = clock;
        this.hashFalso = passwordEncoder.encode(SENHA_FALSA);
    }

    /**
     * noRollbackFor: a contagem de tentativas erradas precisa ser gravada mesmo com a exceção de login inválido.
     */
    @Transactional(noRollbackFor = UnauthorizedException.class)
    public LoginResult login(String login, String senha) {
        Instant agora = clock.instant();
        Usuario usuario = usuarioRepository.findByLogin(login.trim()).orElse(null);

        if (usuario == null || usuario.estaBloqueado(agora) || PasswordPolicy.excedeLimiteDoBcrypt(senha)) {
            gastarTempoDeVerificacao();
            throw new UnauthorizedException(MENSAGEM_LOGIN_INVALIDO);
        }

        if (!passwordEncoder.matches(senha, usuario.getSenhaHash())) {
            usuario.registrarFalhaDeLogin(agora, MAX_TENTATIVAS, DURACAO_BLOQUEIO);
            throw new UnauthorizedException(MENSAGEM_LOGIN_INVALIDO);
        }

        if (!usuario.isAtivo() || !tenantAtivo(usuario.getTenantId())) {
            throw new UnauthorizedException(MENSAGEM_LOGIN_INVALIDO);
        }

        usuario.registrarLoginComSucesso();
        String refreshToken = refreshTokenService.criar(usuario.getId());
        return new LoginResult(gerarResposta(usuario), refreshToken);
    }

    /** Troca o refresh token válido (cookie) por um novo access token. */
    @Transactional(readOnly = true)
    public AuthResponse refresh(String refreshToken) {
        RefreshToken sessao = refreshTokenService.buscarValido(refreshToken)
                .orElseThrow(() -> new UnauthorizedException(MENSAGEM_SESSAO_INVALIDA));
        Usuario usuario = usuarioRepository.findAtivoById(sessao.getUsuarioId())
                .orElseThrow(() -> new UnauthorizedException(MENSAGEM_SESSAO_INVALIDA));
        return gerarResposta(usuario);
    }

    public void logout(String refreshToken) {
        refreshTokenService.revogar(refreshToken);
    }

    /**
     * Conclui o primeiro acesso: troca a senha temporária (substitui o hash em senha_hash) e grava
     * nome completo, e-mail e telefone.
     */
    @Transactional
    public void concluirPrimeiroAcesso(Long usuarioId, PrimeiroAcessoRequest dados) {
        Usuario usuario = usuarioRepository.findAtivoById(usuarioId)
                .orElseThrow(() -> new UnauthorizedException(MENSAGEM_SESSAO_INVALIDA));

        if (usuario.isPrimeiroAcessoConcluido()) {
            throw new BusinessException("O primeiro acesso já foi concluído.");
        }

        PasswordPolicy.validar(dados.novaSenha());
        if (passwordEncoder.matches(dados.novaSenha(), usuario.getSenhaHash())) {
            throw new BusinessException("A nova senha deve ser diferente da senha temporária.");
        }

        usuario.concluirPrimeiroAcesso(
                passwordEncoder.encode(dados.novaSenha()),
                dados.nomeCompleto().trim(),
                dados.email().trim().toLowerCase(Locale.ROOT),
                normalizarTelefone(dados.telefone()));
    }

    private AuthResponse gerarResposta(Usuario usuario) {
        AccessToken token = jwtService.generateAccessToken(usuario.getId(), usuario.getTenantId());
        return new AuthResponse(token.value(), token.expiresInSeconds(), !usuario.isPrimeiroAcessoConcluido());
    }

    private boolean tenantAtivo(Long tenantId) {
        return tenantRepository.findById(tenantId).map(Tenant::isAtivo).orElse(false);
    }

    private void gastarTempoDeVerificacao() {
        passwordEncoder.matches(SENHA_FALSA, hashFalso);
    }

    /** Guarda só os dígitos (DDD + número, com ou sem código do país). */
    private String normalizarTelefone(String telefone) {
        String digitos = telefone.replaceAll("\\D", "");
        if (digitos.length() < 10 || digitos.length() > 13) {
            throw new BusinessException("Telefone inválido. Informe o DDD e o número.");
        }
        return digitos;
    }
}
