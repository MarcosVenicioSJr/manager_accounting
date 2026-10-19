package br.com.manageraccounting.modules.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.manageraccounting.core.security.JwtProperties;

/**
 * Refresh token opaco: 32 bytes aleatórios, entregue ao navegador em cookie HttpOnly.
 * No banco fica só o hash SHA-256. A sessão tem validade fixa (8h por padrão), sem renovação.
 */
@Service
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository repository;
    private final JwtProperties properties;
    private final Clock clock;

    public RefreshTokenService(RefreshTokenRepository repository, JwtProperties properties, Clock clock) {
        this.repository = repository;
        this.properties = properties;
        this.clock = clock;
    }

    public Duration getTtl() {
        return properties.refreshTokenTtl();
    }

    /** Cria uma sessão para o usuário e devolve o valor do token (única vez em que ele existe em claro). */
    @Transactional
    public String criar(Long usuarioId) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        Instant agora = clock.instant();
        repository.save(new RefreshToken(usuarioId, hash(token), agora, agora.plus(properties.refreshTokenTtl())));
        return token;
    }

    /** Retorna a sessão se o token existir, não estiver revogado e não tiver expirado. */
    @Transactional(readOnly = true)
    public Optional<RefreshToken> buscarValido(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        Instant agora = clock.instant();
        return repository.findByTokenHash(hash(token)).filter(stored -> stored.estaValido(agora));
    }

    @Transactional
    public void revogar(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        Instant agora = clock.instant();
        repository.findByTokenHash(hash(token)).ifPresent(stored -> stored.revogar(agora));
    }

    /** Derruba todas as sessões do usuário em todos os navegadores. */
    @Transactional
    public void revogarTodos(Long usuarioId) {
        repository.revogarTodosDoUsuario(usuarioId, clock.instant());
    }

    /** Limpeza diária das sessões expiradas há mais de um dia. */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void limparExpirados() {
        repository.apagarExpiradosAte(clock.instant().minus(Duration.ofDays(1)));
    }

    static String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponível", ex);
        }
    }
}
