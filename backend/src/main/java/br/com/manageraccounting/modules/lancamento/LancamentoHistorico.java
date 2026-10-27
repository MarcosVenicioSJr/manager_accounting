package br.com.manageraccounting.modules.lancamento;

import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Uma ação sobre um lançamento, com o estado dele logo depois (JSON de {@link SnapshotLancamento}). */
@Entity
@Table(name = "lancamento_historico")
public class LancamentoHistorico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lancamento_id", nullable = false, updatable = false)
    private Long lancamentoId;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10, updatable = false)
    private AcaoHistorico acao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10, updatable = false)
    private OrigemLancamento origem;

    @Column(name = "usuario_id", updatable = false)
    private Long usuarioId;

    @Column(name = "feito_em", nullable = false, updatable = false)
    private Instant feitoEm;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, updatable = false, columnDefinition = "jsonb")
    private String dados;

    protected LancamentoHistorico() {
    }

    LancamentoHistorico(Long lancamentoId, Long tenantId, AcaoHistorico acao, OrigemLancamento origem,
            Long usuarioId, Instant feitoEm, String dados) {
        this.lancamentoId = lancamentoId;
        this.tenantId = tenantId;
        this.acao = acao;
        this.origem = origem;
        this.usuarioId = usuarioId;
        this.feitoEm = feitoEm;
        this.dados = dados;
    }

    public Long getId() {
        return id;
    }

    public Long getLancamentoId() {
        return lancamentoId;
    }

    public AcaoHistorico getAcao() {
        return acao;
    }

    public OrigemLancamento getOrigem() {
        return origem;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public Instant getFeitoEm() {
        return feitoEm;
    }

    public String getDados() {
        return dados;
    }
}
