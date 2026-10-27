package br.com.manageraccounting.modules.lancamento;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import br.com.manageraccounting.modules.cliente.TextoBusca;
import br.com.manageraccounting.modules.lancamento.comprovante.ArquivoComprovante;

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
 * Lançamento de um cliente. {@code tenantId}, {@code clienteId} e {@code origem} são definidos na criação e nunca
 * mudam. A exclusão é lógica: o registro fica no banco com {@code excluidoEm} preenchido.
 */
@Entity
@Table(name = "lancamento")
public class Lancamento {

    static final int TAMANHO_DESCRICAO = 200;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;

    @Column(name = "cliente_id", nullable = false, updatable = false)
    private Long clienteId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoLancamento tipo;

    @Column(nullable = false)
    private LocalDate data;

    @Column(nullable = false, length = TAMANHO_DESCRICAO)
    private String descricao;

    /** Descrição em minúsculas e sem acentos (ver {@link TextoBusca}). */
    @Column(nullable = false, length = TAMANHO_DESCRICAO)
    private String busca;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private CategoriaLancamento categoria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10, updatable = false)
    private OrigemLancamento origem;

    @Column(name = "comprovante_chave", length = 200)
    private String comprovanteChave;

    @Column(name = "comprovante_nome", length = 200)
    private String comprovanteNome;

    @Column(name = "comprovante_tipo", length = 50)
    private String comprovanteTipo;

    @Column(name = "comprovante_tamanho")
    private Long comprovanteTamanho;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "criado_por_usuario_id", updatable = false)
    private Long criadoPorUsuarioId;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @Column(name = "atualizado_por_usuario_id")
    private Long atualizadoPorUsuarioId;

    @Column(name = "editado_em")
    private Instant editadoEm;

    @Column(name = "excluido_em")
    private Instant excluidoEm;

    @Column(name = "excluido_por_usuario_id")
    private Long excluidoPorUsuarioId;

    @Version
    @Column(nullable = false)
    private long versao;

    protected Lancamento() {
    }

    /** Lançamento feito pelo escritório (backoffice). */
    static Lancamento doEscritorio(Long tenantId, Long clienteId, Long usuarioId, DadosLancamento dados,
            Instant agora) {
        Lancamento l = new Lancamento();
        l.tenantId = tenantId;
        l.clienteId = clienteId;
        l.origem = OrigemLancamento.ESCRITORIO;
        l.criadoEm = agora;
        l.criadoPorUsuarioId = usuarioId;
        l.aplicar(dados);
        l.marcarAtualizacao(usuarioId, agora);
        return l;
    }

    /** @return {@code false} quando os dados são iguais aos atuais (nada muda, nem o selo "Editado") */
    boolean atualizar(DadosLancamento dados, Long usuarioId, Instant agora) {
        if (dados.equals(dadosAtuais())) {
            return false;
        }
        aplicar(dados);
        marcarEdicao(usuarioId, agora);
        return true;
    }

    void anexarComprovante(ArquivoComprovante arquivo, Long usuarioId, Instant agora) {
        this.comprovanteChave = arquivo.chave();
        this.comprovanteNome = arquivo.nomeOriginal();
        this.comprovanteTipo = arquivo.tipo().getContentType();
        this.comprovanteTamanho = arquivo.tamanho();
        marcarEdicao(usuarioId, agora);
    }

    void removerComprovante(Long usuarioId, Instant agora) {
        this.comprovanteChave = null;
        this.comprovanteNome = null;
        this.comprovanteTipo = null;
        this.comprovanteTamanho = null;
        marcarEdicao(usuarioId, agora);
    }

    void excluir(Long usuarioId, Instant agora) {
        this.excluidoEm = agora;
        this.excluidoPorUsuarioId = usuarioId;
        marcarAtualizacao(usuarioId, agora);
    }

    DadosLancamento dadosAtuais() {
        return new DadosLancamento(tipo, data, descricao, valor, categoria);
    }

    boolean temComprovante() {
        return comprovanteChave != null;
    }

    boolean isExcluido() {
        return excluidoEm != null;
    }

    private void aplicar(DadosLancamento dados) {
        this.tipo = dados.tipo();
        this.data = dados.data();
        this.descricao = dados.descricao();
        String normalizada = TextoBusca.normalizar(dados.descricao());
        this.busca = normalizada.length() > TAMANHO_DESCRICAO
                ? normalizada.substring(0, TAMANHO_DESCRICAO)
                : normalizada;
        this.valor = dados.valor();
        this.categoria = dados.categoria();
    }

    private void marcarEdicao(Long usuarioId, Instant agora) {
        this.editadoEm = agora;
        marcarAtualizacao(usuarioId, agora);
    }

    private void marcarAtualizacao(Long usuarioId, Instant agora) {
        this.atualizadoEm = agora;
        this.atualizadoPorUsuarioId = usuarioId;
    }

    public Long getId() {
        return id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public TipoLancamento getTipo() {
        return tipo;
    }

    public LocalDate getData() {
        return data;
    }

    public String getDescricao() {
        return descricao;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public CategoriaLancamento getCategoria() {
        return categoria;
    }

    public OrigemLancamento getOrigem() {
        return origem;
    }

    public String getComprovanteChave() {
        return comprovanteChave;
    }

    public String getComprovanteNome() {
        return comprovanteNome;
    }

    public String getComprovanteTipo() {
        return comprovanteTipo;
    }

    public Long getComprovanteTamanho() {
        return comprovanteTamanho;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Long getCriadoPorUsuarioId() {
        return criadoPorUsuarioId;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }

    public Instant getEditadoEm() {
        return editadoEm;
    }

    public Instant getExcluidoEm() {
        return excluidoEm;
    }

    public long getVersao() {
        return versao;
    }
}
