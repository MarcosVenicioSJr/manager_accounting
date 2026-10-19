package br.com.manageraccounting.modules.cliente;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Token que o cliente usa para entrar no app. No banco fica só o hash SHA-256.
 * Gerar um novo revoga o anterior; desativar o cliente também revoga.
 */
@Entity
@Table(name = "cliente_token_acesso")
public class ClienteTokenAcesso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cliente_id", nullable = false, updatable = false)
    private Long clienteId;

    @Column(name = "token_hash", nullable = false, updatable = false, length = 64)
    private String tokenHash;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    /** Contador que gerou o token. */
    @Column(name = "criado_por", nullable = false, updatable = false)
    private Long criadoPor;

    @Column(name = "revogado_em")
    private Instant revogadoEm;

    @Column(name = "ultimo_uso_em")
    private Instant ultimoUsoEm;

    protected ClienteTokenAcesso() {
    }

    ClienteTokenAcesso(Long clienteId, String tokenHash, Long criadoPor, Instant agora) {
        this.clienteId = clienteId;
        this.tokenHash = tokenHash;
        this.criadoPor = criadoPor;
        this.criadoEm = agora;
    }

    public Long getId() {
        return id;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Long getCriadoPor() {
        return criadoPor;
    }

    public Instant getRevogadoEm() {
        return revogadoEm;
    }

    public Instant getUltimoUsoEm() {
        return ultimoUsoEm;
    }
}
