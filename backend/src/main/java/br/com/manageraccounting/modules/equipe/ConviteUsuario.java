package br.com.manageraccounting.modules.equipe;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Convite para um usuário do escritório criar a senha. Guarda só o hash SHA-256 do token; o valor existe apenas no
 * link do e-mail. Uso único e com validade (48 horas por padrão). Reenviar anula o convite anterior.
 */
@Entity
@Table(name = "convite_usuario")
public class ConviteUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false, updatable = false)
    private Long usuarioId;

    @Column(name = "token_hash", nullable = false, updatable = false, length = 64)
    private String tokenHash;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    /** Administrador que enviou o convite. */
    @Column(name = "criado_por", nullable = false, updatable = false)
    private Long criadoPor;

    @Column(name = "expira_em", nullable = false, updatable = false)
    private Instant expiraEm;

    @Column(name = "aceito_em")
    private Instant aceitoEm;

    @Column(name = "invalidado_em")
    private Instant invalidadoEm;

    protected ConviteUsuario() {
    }

    ConviteUsuario(Long usuarioId, String tokenHash, Long criadoPor, Instant criadoEm, Instant expiraEm) {
        this.usuarioId = usuarioId;
        this.tokenHash = tokenHash;
        this.criadoPor = criadoPor;
        this.criadoEm = criadoEm;
        this.expiraEm = expiraEm;
    }

    public boolean estaValido(Instant agora) {
        return aceitoEm == null && invalidadoEm == null && expiraEm.isAfter(agora);
    }

    void aceitar(Instant agora) {
        aceitoEm = agora;
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

    public Long getCriadoPor() {
        return criadoPor;
    }

    public Instant getExpiraEm() {
        return expiraEm;
    }

    public Instant getAceitoEm() {
        return aceitoEm;
    }

    public Instant getInvalidadoEm() {
        return invalidadoEm;
    }
}
