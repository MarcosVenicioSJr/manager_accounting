package br.com.manageraccounting.modules.auth;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Pedido de redefinição de senha. Guarda só o hash SHA-256 do token; o valor original existe apenas no link
 * enviado por e-mail. Vale uma única vez e por tempo limitado (30 minutos por padrão).
 */
@Entity
@Table(name = "token_redefinicao_senha")
public class TokenRedefinicaoSenha {

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

    @Column(name = "utilizado_em")
    private Instant utilizadoEm;

    @Column(name = "invalidado_em")
    private Instant invalidadoEm;

    protected TokenRedefinicaoSenha() {
    }

    public TokenRedefinicaoSenha(Long usuarioId, String tokenHash, Instant criadoEm, Instant expiraEm) {
        this.usuarioId = usuarioId;
        this.tokenHash = tokenHash;
        this.criadoEm = criadoEm;
        this.expiraEm = expiraEm;
    }

    public boolean estaValido(Instant agora) {
        return utilizadoEm == null && invalidadoEm == null && expiraEm.isAfter(agora);
    }

    public void marcarComoUtilizado(Instant agora) {
        utilizadoEm = agora;
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

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getExpiraEm() {
        return expiraEm;
    }

    public Instant getUtilizadoEm() {
        return utilizadoEm;
    }

    public Instant getInvalidadoEm() {
        return invalidadoEm;
    }
}
