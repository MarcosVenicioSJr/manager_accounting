package br.com.manageraccounting.modules.auth;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** Encerra todas as sessões abertas do usuário (ex.: depois de redefinir a senha). */
    @Modifying(flushAutomatically = true)
    @Query("""
            update RefreshToken t set t.revogadoEm = :agora
            where t.usuarioId = :usuarioId and t.revogadoEm is null and t.expiraEm > :agora
            """)
    int revogarTodosDoUsuario(@Param("usuarioId") Long usuarioId, @Param("agora") Instant agora);

    @Modifying
    @Query("delete from RefreshToken t where t.expiraEm < :limite")
    int apagarExpiradosAte(@Param("limite") Instant limite);
}
