package br.com.manageraccounting.modules.lancamento;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LancamentoHistoricoRepository extends JpaRepository<LancamentoHistorico, Long> {

    /** Do mais antigo para o mais recente. */
    List<LancamentoHistorico> findByLancamentoIdAndTenantIdOrderByFeitoEmAscIdAsc(Long lancamentoId, Long tenantId);
}
