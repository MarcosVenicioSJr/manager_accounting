package br.com.manageraccounting.modules.cliente;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

/**
 * Toda consulta recebe o tenant: não existe busca de cliente só pelo id.
 */
public interface ClienteRepository extends JpaRepository<Cliente, Long>, JpaSpecificationExecutor<Cliente> {

    Optional<Cliente> findByIdAndTenantId(Long id, Long tenantId);

    /** Mesmo que {@link #findByIdAndTenantId}, com trava de linha (usado ao gerar o token do app). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cliente c where c.id = :id and c.tenantId = :tenantId")
    Optional<Cliente> findForUpdate(@Param("id") Long id, @Param("tenantId") Long tenantId);

    boolean existsByTenantIdAndDocumento(Long tenantId, String documento);

    boolean existsByTenantIdAndDocumentoAndIdNot(Long tenantId, String documento, Long id);

    long countByTenantIdAndAtivo(Long tenantId, boolean ativo);
}
