package br.com.manageraccounting.modules.cliente;

import java.time.Instant;

import br.com.manageraccounting.modules.cliente.dto.EnderecoDto;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Cliente de um escritório (tenant). O {@code tenantId} é definido na criação, a partir do token do contador,
 * e nunca muda.
 */
@Entity
@Table(name = "cliente")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_pessoa", nullable = false, length = 8)
    private TipoPessoa tipoPessoa;

    @Column(nullable = false, length = 14)
    private String documento;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(name = "nome_fantasia", length = 150)
    private String nomeFantasia;

    @Column(length = 150)
    private String email;

    @Column(length = 11)
    private String telefone;

    @Enumerated(EnumType.STRING)
    @Column(name = "regime_tributario", length = 20)
    private RegimeTributario regimeTributario;

    @Column(length = 1000)
    private String observacoes;

    @Column(length = 8)
    private String cep;

    @Column(length = 150)
    private String logradouro;

    @Column(length = 20)
    private String numero;

    @Column(length = 100)
    private String complemento;

    @Column(length = 100)
    private String bairro;

    @Column(length = 100)
    private String cidade;

    @Column(length = 2)
    private String uf;

    /** Nome, nome fantasia e e-mail em minúsculas e sem acentos (ver {@link TextoBusca}). */
    @Column(nullable = false, length = 500)
    private String busca;

    @Column(nullable = false)
    private boolean ativo;

    @Column(name = "data_inclusao", nullable = false, updatable = false)
    private Instant dataInclusao;

    @Column(name = "data_atualizacao", nullable = false)
    private Instant dataAtualizacao;

    @Version
    @Column(nullable = false)
    private long versao;

    protected Cliente() {
    }

    Cliente(Long tenantId, DadosCliente dados, Instant agora) {
        this.tenantId = tenantId;
        this.ativo = true;
        this.dataInclusao = agora;
        aplicar(dados, agora);
    }

    void atualizar(DadosCliente dados, Instant agora) {
        aplicar(dados, agora);
    }

    void desativar(Instant agora) {
        this.ativo = false;
        this.dataAtualizacao = agora;
    }

    void reativar(Instant agora) {
        this.ativo = true;
        this.dataAtualizacao = agora;
    }

    private void aplicar(DadosCliente dados, Instant agora) {
        this.tipoPessoa = dados.tipoPessoa();
        this.documento = dados.documento();
        this.nome = dados.nome();
        this.nomeFantasia = dados.nomeFantasia();
        this.email = dados.email();
        this.telefone = dados.telefone();
        this.regimeTributario = dados.regimeTributario();
        this.observacoes = dados.observacoes();
        this.cep = dados.endereco().cep();
        this.logradouro = dados.endereco().logradouro();
        this.numero = dados.endereco().numero();
        this.complemento = dados.endereco().complemento();
        this.bairro = dados.endereco().bairro();
        this.cidade = dados.endereco().cidade();
        this.uf = dados.endereco().uf();
        this.busca = TextoBusca.doCliente(dados.nome(), dados.nomeFantasia(), dados.email());
        this.dataAtualizacao = agora;
    }

    public Long getId() {
        return id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public TipoPessoa getTipoPessoa() {
        return tipoPessoa;
    }

    public String getDocumento() {
        return documento;
    }

    public String getNome() {
        return nome;
    }

    public String getNomeFantasia() {
        return nomeFantasia;
    }

    public String getEmail() {
        return email;
    }

    public String getTelefone() {
        return telefone;
    }

    public RegimeTributario getRegimeTributario() {
        return regimeTributario;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public EnderecoDto getEndereco() {
        return new EnderecoDto(cep, logradouro, numero, complemento, bairro, cidade, uf);
    }

    public String getCidade() {
        return cidade;
    }

    public String getUf() {
        return uf;
    }

    public String getBusca() {
        return busca;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public Instant getDataInclusao() {
        return dataInclusao;
    }

    public Instant getDataAtualizacao() {
        return dataAtualizacao;
    }

    public long getVersao() {
        return versao;
    }
}
