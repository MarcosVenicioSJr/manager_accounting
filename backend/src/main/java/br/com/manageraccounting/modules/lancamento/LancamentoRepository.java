package br.com.manageraccounting.modules.lancamento;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Toda consulta recebe o tenant: não existe busca de lançamento só pelo id. Excluídos não aparecem aqui.
 */
public interface LancamentoRepository extends JpaRepository<Lancamento, Long>, JpaSpecificationExecutor<Lancamento> {

    Optional<Lancamento> findByIdAndTenantIdAndExcluidoEmIsNull(Long id, Long tenantId);
}
