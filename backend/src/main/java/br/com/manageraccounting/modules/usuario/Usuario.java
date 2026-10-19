package br.com.manageraccounting.modules.usuario;

import java.time.Duration;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Usuário (contador) de um tenant. Cadastrado direto no banco com a senha temporária em {@code senhaHash};
 * no primeiro acesso o hash é trocado e nome, e-mail e telefone são preenchidos.
 */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;

    @Column(nullable = false, length = 60)
    private String login;

    @Column(name = "senha_hash", nullable = false, length = 100)
    private String senhaHash;

    @Column(name = "nome_completo", length = 150)
    private String nomeCompleto;

    @Column(length = 150)
    private String email;

    @Column(length = 20)
    private String telefone;

    @Column(nullable = false)
    private boolean ativo;

    @Column(name = "primeiro_acesso_concluido", nullable = false)
    private boolean primeiroAcessoConcluido;

    @Column(name = "tentativas_falhas", nullable = false)
    private int tentativasFalhas;

    @Column(name = "bloqueado_ate")
    private Instant bloqueadoAte;

    @Column(name = "data_inclusao", nullable = false, updatable = false)
    private Instant dataInclusao;

    protected Usuario() {
    }

    public boolean estaBloqueado(Instant agora) {
        return bloqueadoAte != null && bloqueadoAte.isAfter(agora);
    }

    /**
     * Conta uma tentativa de login com senha errada; ao atingir o limite, bloqueia por um período e zera o contador.
     */
    public void registrarFalhaDeLogin(Instant agora, int maxTentativas, Duration duracaoBloqueio) {
        tentativasFalhas++;
        if (tentativasFalhas >= maxTentativas) {
            bloqueadoAte = agora.plus(duracaoBloqueio);
            tentativasFalhas = 0;
        }
    }

    public void registrarLoginComSucesso() {
        tentativasFalhas = 0;
        bloqueadoAte = null;
    }

    public void concluirPrimeiroAcesso(String novoSenhaHash, String nomeCompleto, String email, String telefone) {
        this.senhaHash = novoSenhaHash;
        this.nomeCompleto = nomeCompleto;
        this.email = email;
        this.telefone = telefone;
        this.primeiroAcessoConcluido = true;
    }

    public Long getId() {
        return id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public String getLogin() {
        return login;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public String getNomeCompleto() {
        return nomeCompleto;
    }

    public String getEmail() {
        return email;
    }

    public String getTelefone() {
        return telefone;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public boolean isPrimeiroAcessoConcluido() {
        return primeiroAcessoConcluido;
    }

    public int getTentativasFalhas() {
        return tentativasFalhas;
    }

    public Instant getBloqueadoAte() {
        return bloqueadoAte;
    }

    public Instant getDataInclusao() {
        return dataInclusao;
    }
}
