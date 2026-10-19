package br.com.manageraccounting.modules.auth;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Sessão de refresh. Guarda só o hash SHA-256 do token; o valor original existe apenas no cookie do navegador.
 */
@Entity
@Table(name = "refresh_token")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false, updatable = false)
    private Long usuarioId;

    @Column(name = "token_hash", nullable = false, updatable = false, length = 64)
    private String tokenHash;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "expira_em", nullable = false, updatable = false)
    private Instant expiraEm;

    @Column(name = "revogado_em")
    private Instant revogadoEm;

    protected RefreshToken() {
    }

    public RefreshToken(Long usuarioId, String tokenHash, Instant criadoEm, Instant expiraEm) {
        this.usuarioId = usuarioId;
        this.tokenHash = tokenHash;
        this.criadoEm = criadoEm;
        this.expiraEm = expiraEm;
    }

    public boolean estaValido(Instant agora) {
        return revogadoEm == null && expiraEm.isAfter(agora);
    }

    public void revogar(Instant agora) {
        if (revogadoEm == null) {
            revogadoEm = agora;
        }
    }

    public Long getId() {
        return id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getExpiraEm() {
        return expiraEm;
    }

    public Instant getRevogadoEm() {
        return revogadoEm;
    }
}
