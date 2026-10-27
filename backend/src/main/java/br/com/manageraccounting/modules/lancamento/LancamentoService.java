package br.com.manageraccounting.modules.lancamento;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.manageraccounting.core.exception.BusinessException;
import br.com.manageraccounting.core.exception.ConflictException;
import br.com.manageraccounting.core.exception.NotFoundException;
import br.com.manageraccounting.modules.cliente.Cliente;
import br.com.manageraccounting.modules.cliente.ClienteRepository;
import br.com.manageraccounting.modules.cliente.TextoBusca;
import br.com.manageraccounting.modules.lancamento.LancamentoTotais.Total;
import br.com.manageraccounting.modules.lancamento.comprovante.ArquivoComprovante;
import br.com.manageraccounting.modules.lancamento.comprovante.ComprovanteProperties;
import br.com.manageraccounting.modules.lancamento.comprovante.ComprovanteStorage;
import br.com.manageraccounting.modules.lancamento.comprovante.NomeArquivo;
import br.com.manageraccounting.modules.lancamento.comprovante.TipoComprovante;
import br.com.manageraccounting.modules.lancamento.dto.CategoriaResponse;
import br.com.manageraccounting.modules.lancamento.dto.ComprovanteResponse;
import br.com.manageraccounting.modules.lancamento.dto.HistoricoResponse;
import br.com.manageraccounting.modules.lancamento.dto.LancamentoRequest;
import br.com.manageraccounting.modules.lancamento.dto.LancamentoResponse;
import br.com.manageraccounting.modules.lancamento.dto.ListaLancamentosResponse;
import br.com.manageraccounting.modules.lancamento.dto.TotalTipoResponse;
import br.com.manageraccounting.modules.usuario.Usuario;
import br.com.manageraccounting.modules.usuario.UsuarioRepository;
import jakarta.persistence.criteria.Predicate;

/**
 * Regras dos lançamentos pelo backoffice. Todo método recebe o tenant do contador logado (vindo do token) e só
 * enxerga lançamentos desse tenant: lançamento de outro escritório responde "não encontrado", igual a um id que não
 * existe. Qualquer usuário do escritório (administrador ou não) pode lançar, editar e excluir lançamentos dos
 * clientes do escritório, inclusive os que o cliente fez pelo app. Cada ação fica no histórico.
 */
@Service
public class LancamentoService {

    static final int TAMANHO_PADRAO = 20;
    static final int TAMANHO_MAXIMO = 100;
    static final LocalDate DATA_MINIMA = LocalDate.of(2000, 1, 1);
    static final String CODIGO_EDICAO_SIMULTANEA = "EDICAO_SIMULTANEA";
    static final String MSG_NAO_ENCONTRADO = "Lançamento não encontrado.";
    static final String MSG_CLIENTE_NAO_ENCONTRADO = "Cliente não encontrado.";
    static final String MSG_EDICAO_SIMULTANEA =
            "Este lançamento foi alterado por outra pessoa depois que você abriu a tela. Recarregue para ver a versão atual.";
    static final String MSG_FORMATO = "Formato não aceito. Envie um PDF ou uma imagem JPG, PNG ou WEBP.";
    static final String MSG_SEM_COMPROVANTE = "Este lançamento não tem comprovante.";

    private static final Logger log = LoggerFactory.getLogger(LancamentoService.class);

    private final LancamentoRepository repository;
    private final LancamentoHistoricoRepository historicoRepository;
    private final LancamentoTotais totais;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final ComprovanteStorage storage;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final ZoneId fuso;
    private final long tamanhoMaximoComprovante;

    public LancamentoService(LancamentoRepository repository, LancamentoHistoricoRepository historicoRepository,
            LancamentoTotais totais, ClienteRepository clienteRepository, UsuarioRepository usuarioRepository,
            ComprovanteStorage storage, ObjectMapper objectMapper, Clock clock, LancamentoProperties properties,
            ComprovanteProperties comprovanteProperties) {
        this.repository = repository;
        this.historicoRepository = historicoRepository;
        this.totais = totais;
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.storage = storage;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.fuso = properties.fusoHorario();
        this.tamanhoMaximoComprovante = comprovanteProperties.tamanhoMaximo().toBytes();
    }

    // ---------- consulta ----------

    public List<CategoriaResponse> categorias() {
        return Arrays.stream(CategoriaLancamento.values())
                .map(c -> new CategoriaResponse(c.name(), c.getRotulo()))
                .toList();
    }

    /** Lista do mais recente para o mais antigo (data, depois ordem de cadastro), com os totais do filtro. */
    @Transactional(readOnly = true)
    public ListaLancamentosResponse listar(Long tenantId, LancamentoFiltro filtro, int pagina, int tamanho) {
        LancamentoFiltro f = filtro == null ? LancamentoFiltro.vazio() : filtro;
        if (f.de() != null && f.ate() != null && f.de().isAfter(f.ate())) {
            throw new BusinessException("A data inicial é depois da data final.");
        }
        int paginaValida = Math.max(pagina, 0);
        int tamanhoValido = tamanho < 1 ? TAMANHO_PADRAO : Math.min(tamanho, TAMANHO_MAXIMO);
        Sort ordem = Sort.by(Sort.Order.desc("data"), Sort.Order.desc("id"));

        Specification<Lancamento> spec = especificacao(tenantId, f);
        Page<Lancamento> page = repository.findAll(spec, PageRequest.of(paginaValida, tamanhoValido, ordem));
        Map<TipoLancamento, Total> porTipo = totais.porTipo(spec);

        List<TotalTipoResponse> totaisPorTipo = Arrays.stream(TipoLancamento.values())
                .map(t -> {
                    Total total = porTipo.getOrDefault(t, Total.ZERO);
                    return new TotalTipoResponse(t, total.quantidade(), total.valor());
                })
                .toList();
        BigDecimal valorTotal = totaisPorTipo.stream()
                .map(TotalTipoResponse::valor)
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);

        return new ListaLancamentosResponse(toResponses(page.getContent()), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), totaisPorTipo, valorTotal);
    }

    @Transactional(readOnly = true)
    public LancamentoResponse obter(Long tenantId, Long id) {
        return toResponse(buscar(tenantId, id));
    }

    /** Histórico do mais antigo para o mais recente. Lançamentos excluídos também têm histórico, mas não por aqui. */
    @Transactional(readOnly = true)
    public List<HistoricoResponse> historico(Long tenantId, Long id) {
        buscar(tenantId, id);
        List<LancamentoHistorico> linhas =
                historicoRepository.findByLancamentoIdAndTenantIdOrderByFeitoEmAscIdAsc(id, tenantId);
        Map<Long, String> nomes = nomesDeUsuarios(linhas.stream().map(LancamentoHistorico::getUsuarioId).toList());
        return linhas.stream()
                .map(h -> new HistoricoResponse(h.getId(), h.getAcao(), h.getOrigem(), nomes.get(h.getUsuarioId()),
                        h.getFeitoEm(), lerSnapshot(h.getDados())))
                .toList();
    }

    // ---------- cadastro ----------

    @Transactional
    public LancamentoResponse criar(Long tenantId, Long usuarioId, LancamentoRequest request) {
        if (request.clienteId() == null) {
            throw new BusinessException("Informe o cliente.");
        }
        Cliente cliente = clienteRepository.findByIdAndTenantId(request.clienteId(), tenantId)
                .orElseThrow(() -> new NotFoundException(MSG_CLIENTE_NAO_ENCONTRADO));
        if (!cliente.isAtivo()) {
            throw new BusinessException("Este cliente está inativo. Reative-o antes de lançar.");
        }
        DadosLancamento dados = normalizar(request, hoje());
        Lancamento lancamento = Lancamento.doEscritorio(tenantId, cliente.getId(), usuarioId, dados, clock.instant());
        salvar(lancamento);
        registrar(lancamento, AcaoHistorico.CRIAR, usuarioId);
        log.info("Lançamento criado: tenant={} cliente={} lancamento={} usuario={}",
                tenantId, cliente.getId(), lancamento.getId(), usuarioId);
        return toResponse(lancamento);
    }

    @Transactional
    public LancamentoResponse atualizar(Long tenantId, Long usuarioId, Long id, LancamentoRequest request) {
        Lancamento lancamento = buscarParaAlterar(tenantId, id, request.versao());
        if (request.clienteId() != null && !request.clienteId().equals(lancamento.getClienteId())) {
            throw new BusinessException("Um lançamento não pode mudar de cliente. Exclua e lance de novo no cliente certo.");
        }
        DadosLancamento dados = normalizar(request, hoje());
        if (lancamento.atualizar(dados, usuarioId, clock.instant())) {
            salvar(lancamento);
            registrar(lancamento, AcaoHistorico.EDITAR, usuarioId);
        }
        return toResponse(lancamento);
    }

    /** Exclusão lógica: some das listas e dos totais, mas fica no banco com o histórico (e com o comprovante). */
    @Transactional
    public void excluir(Long tenantId, Long usuarioId, Long id, Long versao) {
        Lancamento lancamento = buscarParaAlterar(tenantId, id, versao);
        lancamento.excluir(usuarioId, clock.instant());
        salvar(lancamento);
        registrar(lancamento, AcaoHistorico.EXCLUIR, usuarioId);
        log.info("Lançamento excluído: tenant={} lancamento={} usuario={}", tenantId, id, usuarioId);
    }

    // ---------- comprovante ----------

    /**
     * Anexa (ou troca) o comprovante. O formato vem dos primeiros bytes do arquivo. O arquivo novo só fica se a
     * transação der certo, e o antigo só é apagado depois que ela é confirmada.
     */
    @Transactional
    public LancamentoResponse anexarComprovante(Long tenantId, Long usuarioId, Long id, Long versao,
            MultipartFile arquivo) {
        Lancamento lancamento = buscarParaAlterar(tenantId, id, versao);
        if (arquivo == null || arquivo.isEmpty()) {
            throw new BusinessException("Escolha o arquivo do comprovante.");
        }
        if (arquivo.getSize() > tamanhoMaximoComprovante) {
            throw new BusinessException("Arquivo muito grande. O limite é de "
                    + (tamanhoMaximoComprovante / (1024 * 1024)) + " MB.");
        }

        ArquivoComprovante novo;
        try (InputStream entrada = new BufferedInputStream(arquivo.getInputStream())) {
            entrada.mark(TipoComprovante.BYTES_ASSINATURA);
            byte[] inicio = entrada.readNBytes(TipoComprovante.BYTES_ASSINATURA);
            entrada.reset();
            TipoComprovante tipo = TipoComprovante.detectar(inicio)
                    .orElseThrow(() -> new BusinessException(MSG_FORMATO));
            String chave = storage.gravar(tenantId, tipo, entrada);
            novo = new ArquivoComprovante(chave, NomeArquivo.limpar(arquivo.getOriginalFilename(), tipo), tipo,
                    arquivo.getSize());
        } catch (IOException ex) {
            throw new UncheckedIOException("Falha ao gravar o comprovante", ex);
        }
        apagarSeDerErrado(novo.chave());

        String anterior = lancamento.getComprovanteChave();
        lancamento.anexarComprovante(novo, usuarioId, clock.instant());
        salvar(lancamento);
        registrar(lancamento, AcaoHistorico.EDITAR, usuarioId);
        if (anterior != null) {
            apagarDepoisDeConfirmar(anterior);
        }
        log.info("Comprovante anexado: tenant={} lancamento={} tipo={} tamanho={} usuario={}",
                tenantId, id, novo.tipo(), novo.tamanho(), usuarioId);
        return toResponse(lancamento);
    }

    @Transactional
    public LancamentoResponse removerComprovante(Long tenantId, Long usuarioId, Long id, Long versao) {
        Lancamento lancamento = buscarParaAlterar(tenantId, id, versao);
        if (!lancamento.temComprovante()) {
            return toResponse(lancamento);
        }
        String chave = lancamento.getComprovanteChave();
        lancamento.removerComprovante(usuarioId, clock.instant());
        salvar(lancamento);
        registrar(lancamento, AcaoHistorico.EDITAR, usuarioId);
        apagarDepoisDeConfirmar(chave);
        return toResponse(lancamento);
    }

    /** Arquivo do comprovante para download. Quem chama fecha o {@code conteudo}. */
    @Transactional(readOnly = true)
    public ComprovanteDownload baixarComprovante(Long tenantId, Long id) {
        Lancamento lancamento = buscar(tenantId, id);
        if (!lancamento.temComprovante()) {
            throw new NotFoundException(MSG_SEM_COMPROVANTE);
        }
        try {
            return new ComprovanteDownload(lancamento.getComprovanteNome(), lancamento.getComprovanteTipo(),
                    lancamento.getComprovanteTamanho(), storage.abrir(lancamento.getComprovanteChave()));
        } catch (IOException ex) {
            log.error("Comprovante não encontrado no armazenamento: tenant={} lancamento={}", tenantId, id, ex);
            throw new NotFoundException("O arquivo do comprovante não foi encontrado.");
        }
    }

    public record ComprovanteDownload(String nome, String contentType, long tamanho, InputStream conteudo) {
    }

    // ---------- apoio ----------

    private LocalDate hoje() {
        return LocalDate.now(clock.withZone(fuso));
    }

    private Lancamento buscar(Long tenantId, Long id) {
        return repository.findByIdAndTenantIdAndExcluidoEmIsNull(id, tenantId)
                .orElseThrow(() -> new NotFoundException(MSG_NAO_ENCONTRADO));
    }

    private Lancamento buscarParaAlterar(Long tenantId, Long id, Long versao) {
        if (versao == null) {
            throw new BusinessException("Informe a versão do lançamento que está sendo alterada.");
        }
        Lancamento lancamento = buscar(tenantId, id);
        if (lancamento.getVersao() != versao) {
            throw new ConflictException(CODIGO_EDICAO_SIMULTANEA, MSG_EDICAO_SIMULTANEA);
        }
        return lancamento;
    }

    private void salvar(Lancamento lancamento) {
        try {
            repository.saveAndFlush(lancamento);
        } catch (OptimisticLockingFailureException ex) {
            throw new ConflictException(CODIGO_EDICAO_SIMULTANEA, MSG_EDICAO_SIMULTANEA);
        }
    }

    private void registrar(Lancamento l, AcaoHistorico acao, Long usuarioId) {
        historicoRepository.save(new LancamentoHistorico(l.getId(), l.getTenantId(), acao, OrigemLancamento.ESCRITORIO,
                usuarioId, l.getAtualizadoEm(), escreverSnapshot(SnapshotLancamento.de(l))));
    }

    private String escreverSnapshot(SnapshotLancamento snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Falha ao montar o histórico do lançamento", ex);
        }
    }

    private SnapshotLancamento lerSnapshot(String json) {
        try {
            return objectMapper.readValue(json, SnapshotLancamento.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Histórico do lançamento ilegível", ex);
        }
    }

    /** Se a transação for desfeita, o arquivo recém-gravado não serve para nada: apaga. */
    private void apagarSeDerErrado(String chave) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    apagarArquivo(chave);
                }
            }
        });
    }

    /** O arquivo substituído ou removido só sai do disco depois que o banco confirmou a mudança. */
    private void apagarDepoisDeConfirmar(String chave) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            apagarArquivo(chave);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                apagarArquivo(chave);
            }
        });
    }

    private void apagarArquivo(String chave) {
        try {
            storage.apagar(chave);
        } catch (IOException | RuntimeException ex) {
            // Sobra um arquivo sem uso no disco; não vale derrubar a operação por isso
            log.warn("Não foi possível apagar o comprovante {}", chave, ex);
        }
    }

    private List<LancamentoResponse> toResponses(List<Lancamento> lancamentos) {
        Map<Long, String> clientes = nomesDeClientes(lancamentos.stream().map(Lancamento::getClienteId).toList());
        Map<Long, String> usuarios = nomesDeUsuarios(
                lancamentos.stream().map(Lancamento::getCriadoPorUsuarioId).toList());
        return lancamentos.stream().map(l -> toResponse(l, clientes, usuarios)).toList();
    }

    private LancamentoResponse toResponse(Lancamento l) {
        return toResponses(List.of(l)).get(0);
    }

    private static LancamentoResponse toResponse(Lancamento l, Map<Long, String> clientes,
            Map<Long, String> usuarios) {
        ComprovanteResponse comprovante = l.temComprovante()
                ? new ComprovanteResponse(l.getComprovanteNome(), l.getComprovanteTipo(), l.getComprovanteTamanho())
                : null;
        return new LancamentoResponse(l.getId(), l.getClienteId(), clientes.get(l.getClienteId()), l.getTipo(),
                l.getData(), l.getDescricao(), l.getValor(), l.getCategoria(), l.getOrigem(), comprovante,
                l.getCriadoEm(), usuarios.get(l.getCriadoPorUsuarioId()), l.getEditadoEm(), l.getVersao());
    }

    private Map<Long, String> nomesDeClientes(Collection<Long> ids) {
        Set<Long> unicos = semNulos(ids);
        if (unicos.isEmpty()) {
            return Map.of();
        }
        return clienteRepository.findAllById(unicos).stream()
                .collect(Collectors.toMap(Cliente::getId, Cliente::getNome));
    }

    private Map<Long, String> nomesDeUsuarios(Collection<Long> ids) {
        Set<Long> unicos = semNulos(ids);
        if (unicos.isEmpty()) {
            return Map.of();
        }
        return usuarioRepository.findAllById(unicos).stream()
                .collect(Collectors.toMap(Usuario::getId, LancamentoService::nomeDoUsuario));
    }

    /** Usuário que ainda não concluiu o primeiro acesso não tem nome completo: usa o login. */
    private static String nomeDoUsuario(Usuario u) {
        return u.getNomeCompleto() != null && !u.getNomeCompleto().isBlank() ? u.getNomeCompleto() : u.getLogin();
    }

    private static Set<Long> semNulos(Collection<Long> ids) {
        Set<Long> unicos = new HashSet<>(ids);
        unicos.remove(null);
        return unicos;
    }

    /**
     * Valida e limpa: descrição sem espaços nas pontas (e sem espaços repetidos), valor com duas casas, data entre
     * 01/01/2000 e hoje (no fuso do escritório).
     */
    static DadosLancamento normalizar(LancamentoRequest request, LocalDate hoje) {
        if (request.tipo() == null) {
            throw new BusinessException("Informe o tipo do lançamento.");
        }
        if (request.categoria() == null) {
            throw new BusinessException("Informe a categoria.");
        }
        String descricao = request.descricao() == null ? "" : request.descricao().strip().replaceAll("\\s+", " ");
        if (descricao.isEmpty()) {
            throw new BusinessException("Informe a descrição.");
        }
        if (descricao.length() > Lancamento.TAMANHO_DESCRICAO) {
            throw new BusinessException("Descrição muito longa (máximo de 200 caracteres).");
        }

        BigDecimal valor = request.valor();
        if (valor == null || valor.signum() <= 0) {
            throw new BusinessException("O valor precisa ser maior que zero.");
        }
        if (valor.stripTrailingZeros().scale() > 2) {
            throw new BusinessException("Use no máximo duas casas decimais no valor.");
        }
        valor = valor.setScale(2, RoundingMode.UNNECESSARY);
        if (valor.precision() - valor.scale() > 12) {
            throw new BusinessException("Valor muito alto.");
        }

        LocalDate data = request.data();
        if (data == null) {
            throw new BusinessException("Informe a data.");
        }
        if (data.isAfter(hoje)) {
            throw new BusinessException("A data não pode ser no futuro.");
        }
        if (data.isBefore(DATA_MINIMA)) {
            throw new BusinessException("Data inválida. Confira o ano.");
        }
        return new DadosLancamento(request.tipo(), data, descricao, valor, request.categoria());
    }

    /** Tenant obrigatório, nunca os excluídos, e os filtros que vierem preenchidos. */
    static Specification<Lancamento> especificacao(Long tenantId, LancamentoFiltro f) {
        Objects.requireNonNull(tenantId, "tenantId");
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.equal(root.get("tenantId"), tenantId));
            p.add(cb.isNull(root.get("excluidoEm")));
            if (f.clienteId() != null) {
                p.add(cb.equal(root.get("clienteId"), f.clienteId()));
            }
            if (f.tipo() != null) {
                p.add(cb.equal(root.get("tipo"), f.tipo()));
            }
            if (f.categoria() != null) {
                p.add(cb.equal(root.get("categoria"), f.categoria()));
            }
            if (f.de() != null) {
                p.add(cb.greaterThanOrEqualTo(root.get("data"), f.de()));
            }
            if (f.ate() != null) {
                p.add(cb.lessThanOrEqualTo(root.get("data"), f.ate()));
            }
            String termo = f.busca() == null ? "" : f.busca().strip();
            if (!termo.isEmpty()) {
                String padrao = "%" + escaparLike(TextoBusca.normalizar(termo)) + "%";
                p.add(cb.like(root.get("busca"), padrao, '\\'));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
    }

    private static String escaparLike(String valor) {
        return valor.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
