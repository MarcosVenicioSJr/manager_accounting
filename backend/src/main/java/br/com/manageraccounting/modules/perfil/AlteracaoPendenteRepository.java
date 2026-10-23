package br.com.manageraccounting.modules.perfil;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface AlteracaoPendenteRepository extends JpaRepository<AlteracaoPendente, Long> {

    Optional<AlteracaoPendente> findByTokenHash(String tokenHash);

    /** Busca travando a linha: dois cliques no mesmo link não confirmam duas vezes. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AlteracaoPendente a where a.tokenHash = :tokenHash")
    Optional<AlteracaoPendente> findByTokenHashParaUso(@Param("tokenHash") String tokenHash);

    /** Pedidos ainda esperando confirmação (no máximo um por tipo, porque um pedido novo anula o anterior). */
    @Query("""
            select a from AlteracaoPendente a
            where a.usuarioId = :usuarioId and a.confirmadoEm is null and a.invalidadoEm is null
              and a.expiraEm > :agora
            order by a.criadoEm desc
            """)
    List<AlteracaoPendente> findAbertas(@Param("usuarioId") Long usuarioId, @Param("agora") Instant agora);

    long countByUsuarioIdAndTipoAndCriadoEmAfter(Long usuarioId, TipoAlteracao tipo, Instant desde);

    boolean existsByUsuarioIdAndTipoAndCriadoEmAfter(Long usuarioId, TipoAlteracao tipo, Instant desde);

    /** Anula os pedidos em aberto do tipo (cancelar, pedido novo ou confirmação). */
    @Modifying(flushAutomatically = true)
    @Query("""
            update AlteracaoPendente a set a.invalidadoEm = :agora
            where a.usuarioId = :usuarioId and a.tipo = :tipo
              and a.confirmadoEm is null and a.invalidadoEm is null
            """)
    int invalidarAbertas(@Param("usuarioId") Long usuarioId, @Param("tipo") TipoAlteracao tipo,
            @Param("agora") Instant agora);

    @Modifying
    @Query("delete from AlteracaoPendente a where a.expiraEm < :limite")
    int apagarExpiradasAte(@Param("limite") Instant limite);
}
