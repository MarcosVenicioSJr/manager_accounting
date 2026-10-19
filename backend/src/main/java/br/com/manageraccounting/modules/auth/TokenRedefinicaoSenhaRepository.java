package br.com.manageraccounting.modules.auth;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface TokenRedefinicaoSenhaRepository extends JpaRepository<TokenRedefinicaoSenha, Long> {

    Optional<TokenRedefinicaoSenha> findByTokenHash(String tokenHash);

    /** Busca travando a linha: dois envios simultâneos do mesmo link não conseguem usar o token duas vezes. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TokenRedefinicaoSenha t where t.tokenHash = :tokenHash")
    Optional<TokenRedefinicaoSenha> findByTokenHashParaUso(@Param("tokenHash") String tokenHash);

    /** Quantos pedidos o usuário fez a partir de um instante (limite de envios). */
    long countByUsuarioIdAndCriadoEmAfter(Long usuarioId, Instant desde);

    boolean existsByUsuarioIdAndCriadoEmAfter(Long usuarioId, Instant desde);

    /** Anula os tokens ainda pendentes do usuário (só o link mais recente vale). */
    @Modifying(flushAutomatically = true)
    @Query("""
            update TokenRedefinicaoSenha t set t.invalidadoEm = :agora
            where t.usuarioId = :usuarioId and t.utilizadoEm is null and t.invalidadoEm is null
            """)
    int invalidarPendentes(@Param("usuarioId") Long usuarioId, @Param("agora") Instant agora);

    @Modifying
    @Query("delete from TokenRedefinicaoSenha t where t.expiraEm < :limite")
    int apagarExpiradosAte(@Param("limite") Instant limite);
}
