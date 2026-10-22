package br.com.manageraccounting.modules.equipe;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface ConviteUsuarioRepository extends JpaRepository<ConviteUsuario, Long> {

    Optional<ConviteUsuario> findByTokenHash(String tokenHash);

    /** Busca travando a linha: dois envios do mesmo link não aceitam o convite duas vezes. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ConviteUsuario c where c.tokenHash = :tokenHash")
    Optional<ConviteUsuario> findByTokenHashParaUso(@Param("tokenHash") String tokenHash);

    /** Convites ainda válidos dos usuários informados (para mostrar a validade na lista). */
    @Query("""
            select c from ConviteUsuario c
            where c.usuarioId in :usuarioIds and c.aceitoEm is null and c.invalidadoEm is null
              and c.expiraEm > :agora
            """)
    List<ConviteUsuario> findAbertos(@Param("usuarioIds") Collection<Long> usuarioIds, @Param("agora") Instant agora);

    boolean existsByUsuarioIdAndCriadoEmAfter(Long usuarioId, Instant desde);

    @Modifying(flushAutomatically = true)
    @Query("""
            update ConviteUsuario c set c.invalidadoEm = :agora
            where c.usuarioId = :usuarioId and c.aceitoEm is null and c.invalidadoEm is null
            """)
    int invalidarAbertos(@Param("usuarioId") Long usuarioId, @Param("agora") Instant agora);

    @Modifying
    @Query("delete from ConviteUsuario c where c.expiraEm < :limite and c.aceitoEm is null")
    int apagarExpiradosAte(@Param("limite") Instant limite);
}
