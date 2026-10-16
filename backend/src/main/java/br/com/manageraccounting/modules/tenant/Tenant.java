package br.com.manageraccounting.modules.tenant;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Escritório de contabilidade (tenant). Cadastrado direto no banco.
 */
@Entity
@Table(name = "tenant")
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome_empresa", nullable = false, length = 150)
    private String nomeEmpresa;

    /** Código da empresa: único e imutável. */
    @Column(nullable = false, updatable = false)
    private UUID guid;

    @Column(nullable = false)
    private boolean ativo;

    @Column(name = "data_inclusao", nullable = false, updatable = false)
    private Instant dataInclusao;

    protected Tenant() {
    }

    public Long getId() {
        return id;
    }

    public String getNomeEmpresa() {
        return nomeEmpresa;
    }

    public UUID getGuid() {
        return guid;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public Instant getDataInclusao() {
        return dataInclusao;
    }
}
