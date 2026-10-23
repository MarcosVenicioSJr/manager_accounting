package br.com.manageraccounting.modules.perfil;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Troca de senha ou de e-mail pedida no Meu perfil, esperando a confirmação pelo link do e-mail.
 * Guarda só o hash SHA-256 do token. Vale uma única vez e por tempo limitado (30 minutos por padrão).
 * <ul>
 *   <li>SENHA: {@code valorNovo} é o hash BCrypt da nova senha; {@code referencia} é o SHA-256 do hash da senha
 *   no momento do pedido (se a senha mudar por outro caminho antes, o pedido deixa de valer).</li>
 *   <li>EMAIL: {@code valorNovo} é o novo e-mail; {@code referencia} é o e-mail atual.</li>
 * </ul>
 */
@Entity
@Table(name = "alteracao_pendente")
public class AlteracaoPendente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false, updatable = false)
    private Long usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 10)
    private TipoAlteracao tipo;

    @Column(name = "valor_novo", nullable = false, updatable = false, length = 150)
    private String valorNovo;

    @Column(updatable = false, length = 150)
    private String referencia;

    @Column(name = "token_hash", nullable = false, updatable = false, length = 64)
    private String tokenHash;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "expira_em", nullable = false, updatable = false)
    private Instant expiraEm;

    @Column(name = "confirmado_em")
    private Instant confirmadoEm;

    @Column(name = "invalidado_em")
    private Instant invalidadoEm;

    protected AlteracaoPendente() {
    }

    AlteracaoPendente(Long usuarioId, TipoAlteracao tipo, String valorNovo, String referencia, String tokenHash,
            Instant criadoEm, Instant expiraEm) {
        this.usuarioId = usuarioId;
        this.tipo = tipo;
        this.valorNovo = valorNovo;
        this.referencia = referencia;
        this.tokenHash = tokenHash;
        this.criadoEm = criadoEm;
        this.expiraEm = expiraEm;
    }

    public boolean estaValida(Instant agora) {
        return confirmadoEm == null && invalidadoEm == null && expiraEm.isAfter(agora);
    }

    void confirmar(Instant agora) {
        confirmadoEm = agora;
    }

    public Long getId() {
        return id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public TipoAlteracao getTipo() {
        return tipo;
    }

    public String getValorNovo() {
        return valorNovo;
    }

    public String getReferencia() {
        return referencia;
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

    public Instant getConfirmadoEm() {
        return confirmadoEm;
    }

    public Instant getInvalidadoEm() {
        return invalidadoEm;
    }
}
