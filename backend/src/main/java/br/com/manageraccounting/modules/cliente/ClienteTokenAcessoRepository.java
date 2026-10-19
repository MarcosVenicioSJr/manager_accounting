package br.com.manageraccounting.modules.cliente;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClienteTokenAcessoRepository extends JpaRepository<ClienteTokenAcesso, Long> {

    Optional<ClienteTokenAcesso> findByClienteIdAndRevogadoEmIsNull(Long clienteId);

    /** Ids (entre os informados) que têm token ativo; usado para a coluna "App" da lista. */
    @Query("""
            select t.clienteId from ClienteTokenAcesso t
            where t.clienteId in :clienteIds and t.revogadoEm is null
            """)
    List<Long> findClienteIdsComTokenAtivo(@Param("clienteIds") Collection<Long> clienteIds);

    /** Clientes ativos do tenant com token ativo (resumo da página inicial). */
    @Query("""
            select count(t) from ClienteTokenAcesso t
            join Cliente c on c.id = t.clienteId
            where c.tenantId = :tenantId and c.ativo = true and t.revogadoEm is null
            """)
    long countAtivosDoTenant(@Param("tenantId") Long tenantId);

    @Modifying(flushAutomatically = true)
    @Query("""
            update ClienteTokenAcesso t set t.revogadoEm = :agora
            where t.clienteId = :clienteId and t.revogadoEm is null
            """)
    int revogarAtivos(@Param("clienteId") Long clienteId, @Param("agora") Instant agora);
}
