package br.com.manageraccounting.modules.lancamento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.unit.DataSize;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.manageraccounting.core.exception.BusinessException;
import br.com.manageraccounting.core.exception.ConflictException;
import br.com.manageraccounting.core.exception.NotFoundException;
import br.com.manageraccounting.modules.cliente.Cliente;
import br.com.manageraccounting.modules.cliente.ClienteRepository;
import br.com.manageraccounting.modules.lancamento.LancamentoTotais.Total;
import br.com.manageraccounting.modules.lancamento.comprovante.ComprovanteProperties;
import br.com.manageraccounting.modules.lancamento.comprovante.DiscoComprovanteStorage;
import br.com.manageraccounting.modules.lancamento.dto.LancamentoRequest;
import br.com.manageraccounting.modules.lancamento.dto.LancamentoResponse;
import br.com.manageraccounting.modules.lancamento.dto.ListaLancamentosResponse;
import br.com.manageraccounting.modules.lancamento.dto.TotalTipoResponse;
import br.com.manageraccounting.modules.usuario.Usuario;
import br.com.manageraccounting.modules.usuario.UsuarioRepository;

class LancamentoServiceTest {

    /** 23h de 26/10 em São Paulo, mas já 27/10 em UTC: "hoje" tem de ser 26/10. */
    private static final Instant AGORA = Instant.parse("2026-10-27T02:00:00Z");
    private static final LocalDate HOJE = LocalDate.of(2026, 10, 26);
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final Long TENANT = 10L;
    private static final Long USUARIO = 1L;
    private static final Long CLIENTE = 100L;
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 13, 'I', 'H'};
    private static final byte[] PDF = "%PDF-1.7\n%conteudo de teste".getBytes();

    @TempDir
    Path pasta;

    private LancamentoRepository repository;
    private LancamentoHistoricoRepository historicoRepository;
    private LancamentoTotais totais;
    private ClienteRepository clienteRepository;
    private UsuarioRepository usuarioRepository;
    private LancamentoService service;
    private long proximoId = 500;

    @BeforeEach
    void setUp() {
        repository = mock(LancamentoRepository.class);
        historicoRepository = mock(LancamentoHistoricoRepository.class);
        totais = mock(LancamentoTotais.class);
        clienteRepository = mock(ClienteRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);
        ComprovanteProperties comprovante = new ComprovanteProperties(pasta, DataSize.ofKilobytes(1));
        service = new LancamentoService(repository, historicoRepository, totais, clienteRepository, usuarioRepository,
                new DiscoComprovanteStorage(comprovante), new ObjectMapper(), Clock.fixed(AGORA, ZoneOffset.UTC),
                new LancamentoProperties(FUSO), comprovante);

        // Simula o banco gerando o id na primeira gravação
        when(repository.saveAndFlush(any(Lancamento.class))).thenAnswer(inv -> {
            Lancamento l = inv.getArgument(0);
            if (l.getId() == null) {
                ReflectionTestUtils.setField(l, "id", proximoId++);
            }
            return l;
        });

        Cliente cliente = cliente(CLIENTE, true);
        when(clienteRepository.findByIdAndTenantId(CLIENTE, TENANT)).thenReturn(Optional.of(cliente));
        when(clienteRepository.findAllById(any())).thenReturn(List.of(cliente));
        Usuario usuario = mock(Usuario.class);
        when(usuario.getId()).thenReturn(USUARIO);
        when(usuario.getNomeCompleto()).thenReturn("Ana Contadora");
        when(usuarioRepository.findAllById(any())).thenReturn(List.of(usuario));
    }

    // ---------- cadastro ----------

    @Test
    void criarUsaOTenantDoTokenELimpaOsDados() {
        LancamentoResponse criado = service.criar(TENANT, USUARIO, request(CLIENTE, "  Café   da manhã  ",
                new BigDecimal("12.5"), HOJE, null));

        ArgumentCaptor<Lancamento> captor = ArgumentCaptor.forClass(Lancamento.class);
        verify(repository).saveAndFlush(captor.capture());
        Lancamento salvo = captor.getValue();
        assertThat(salvo.getTenantId()).isEqualTo(TENANT);
        assertThat(salvo.getClienteId()).isEqualTo(CLIENTE);
        assertThat(salvo.getOrigem()).isEqualTo(OrigemLancamento.ESCRITORIO);
        assertThat(salvo.getDescricao()).isEqualTo("Café da manhã");
        assertThat(ReflectionTestUtils.getField(salvo, "busca")).isEqualTo("cafe da manha");
        assertThat(salvo.getValor()).isEqualTo(new BigDecimal("12.50"));
        assertThat(salvo.getCriadoPorUsuarioId()).isEqualTo(USUARIO);
        assertThat(salvo.getEditadoEm()).isNull();

        assertThat(criado.clienteNome()).isEqualTo("Padaria Pão Quente");
        assertThat(criado.criadoPor()).isEqualTo("Ana Contadora");
        assertThat(criado.comprovante()).isNull();
    }

    @Test
    void criarRegistraNoHistoricoComOsDados() {
        service.criar(TENANT, USUARIO, request(CLIENTE, "Aluguel", new BigDecimal("1500"), HOJE, null));

        ArgumentCaptor<LancamentoHistorico> captor = ArgumentCaptor.forClass(LancamentoHistorico.class);
        verify(historicoRepository).save(captor.capture());
        LancamentoHistorico h = captor.getValue();
        assertThat(h.getAcao()).isEqualTo(AcaoHistorico.CRIAR);
        assertThat(h.getOrigem()).isEqualTo(OrigemLancamento.ESCRITORIO);
        assertThat(h.getUsuarioId()).isEqualTo(USUARIO);
        assertThat(h.getDados()).contains("\"valor\":\"1500.00\"", "\"data\":\"2026-10-26\"",
                "\"descricao\":\"Aluguel\"");
    }

    @Test
    void clienteDeOutroEscritorioNaoEhEncontrado() {
        when(clienteRepository.findByIdAndTenantId(999L, TENANT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.criar(TENANT, USUARIO, request(999L, "X", BigDecimal.TEN, HOJE, null)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage(LancamentoService.MSG_CLIENTE_NAO_ENCONTRADO);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void naoLancaParaClienteInativoNemSemCliente() {
        Cliente inativo = cliente(200L, false);
        when(clienteRepository.findByIdAndTenantId(200L, TENANT)).thenReturn(Optional.of(inativo));

        assertThatThrownBy(() -> service.criar(TENANT, USUARIO, request(200L, "X", BigDecimal.TEN, HOJE, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("inativo");
        assertThatThrownBy(() -> service.criar(TENANT, USUARIO, request(null, "X", BigDecimal.TEN, HOJE, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Informe o cliente.");
    }

    @Test
    void dataNoFuturoUsaOHojeDoFusoDoEscritorio() {
        // Em UTC já é dia 27, mas no escritório ainda é 26
        assertThatThrownBy(() -> service.criar(TENANT, USUARIO,
                request(CLIENTE, "X", BigDecimal.TEN, HOJE.plusDays(1), null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("A data não pode ser no futuro.");
        assertThatThrownBy(() -> service.criar(TENANT, USUARIO,
                request(CLIENTE, "X", BigDecimal.TEN, LocalDate.of(1999, 12, 31), null)))
                .isInstanceOf(BusinessException.class);
        service.criar(TENANT, USUARIO, request(CLIENTE, "X", BigDecimal.TEN, HOJE, null));
    }

    @Test
    void valorPrecisaSerPositivoComAteDuasCasas() {
        assertThatThrownBy(() -> LancamentoService.normalizar(
                request(CLIENTE, "X", BigDecimal.ZERO, HOJE, null), HOJE))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> LancamentoService.normalizar(
                request(CLIENTE, "X", new BigDecimal("10.005"), HOJE, null), HOJE))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("duas casas");
        assertThatThrownBy(() -> LancamentoService.normalizar(
                request(CLIENTE, "X", new BigDecimal("1000000000000.00"), HOJE, null), HOJE))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Valor muito alto.");
        // Zeros sobrando depois da segunda casa não são erro
        assertThat(LancamentoService.normalizar(
                request(CLIENTE, "X", new BigDecimal("10.500"), HOJE, null), HOJE).valor())
                .isEqualTo(new BigDecimal("10.50"));
    }

    @Test
    void descricaoSoComEspacosEhRecusada() {
        assertThatThrownBy(() -> LancamentoService.normalizar(
                request(CLIENTE, "   ", BigDecimal.TEN, HOJE, null), HOJE))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Informe a descrição.");
    }

    // ---------- edição e exclusão ----------

    @Test
    void editarComVersaoAntigaDaConflito() {
        Lancamento existente = existente();
        ReflectionTestUtils.setField(existente, "versao", 3L);

        assertThatThrownBy(() -> service.atualizar(TENANT, USUARIO, existente.getId(),
                request(null, "Outro", BigDecimal.ONE, HOJE, 2L)))
                .isInstanceOf(ConflictException.class)
                .hasMessage(LancamentoService.MSG_EDICAO_SIMULTANEA);
        assertThatThrownBy(() -> service.atualizar(TENANT, USUARIO, existente.getId(),
                request(null, "Outro", BigDecimal.ONE, HOJE, null)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void editarMarcaComoEditadoEGuardaNoHistorico() {
        Lancamento existente = existente();

        LancamentoResponse r = service.atualizar(TENANT, 2L, existente.getId(),
                new LancamentoRequest(CLIENTE, TipoLancamento.DIVIDA, HOJE, "Empréstimo", new BigDecimal("300"),
                        CategoriaLancamento.EMPRESTIMOS_FINANCIAMENTOS, 0L));

        assertThat(r.tipo()).isEqualTo(TipoLancamento.DIVIDA);
        assertThat(r.valor()).isEqualTo(new BigDecimal("300.00"));
        assertThat(r.editadoEm()).isEqualTo(AGORA);
        ArgumentCaptor<LancamentoHistorico> captor = ArgumentCaptor.forClass(LancamentoHistorico.class);
        verify(historicoRepository).save(captor.capture());
        assertThat(captor.getValue().getAcao()).isEqualTo(AcaoHistorico.EDITAR);
        assertThat(captor.getValue().getUsuarioId()).isEqualTo(2L);
    }

    @Test
    void editarSemMudarNadaNaoGravaNemMarcaEditado() {
        Lancamento existente = existente();

        LancamentoResponse r = service.atualizar(TENANT, USUARIO, existente.getId(),
                request(null, "Aluguel de outubro", new BigDecimal("1500.00"), LocalDate.of(2026, 10, 5), 0L));

        assertThat(r.editadoEm()).isNull();
        verify(repository, never()).saveAndFlush(any());
        verify(historicoRepository, never()).save(any());
    }

    @Test
    void lancamentoNaoMudaDeCliente() {
        Lancamento existente = existente();

        assertThatThrownBy(() -> service.atualizar(TENANT, USUARIO, existente.getId(),
                request(777L, "Aluguel", BigDecimal.TEN, HOJE, 0L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("não pode mudar de cliente");
    }

    @Test
    void lancamentoDeOutroEscritorioNaoEhEncontrado() {
        when(repository.findByIdAndTenantIdAndExcluidoEmIsNull(42L, TENANT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obter(TENANT, 42L)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.excluir(TENANT, USUARIO, 42L, 0L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void excluirEhLogicoEFicaNoHistorico() {
        Lancamento existente = existente();

        service.excluir(TENANT, USUARIO, existente.getId(), 0L);

        assertThat(existente.getExcluidoEm()).isEqualTo(AGORA);
        assertThat(ReflectionTestUtils.getField(existente, "excluidoPorUsuarioId")).isEqualTo(USUARIO);
        verify(repository, never()).delete(any());
        ArgumentCaptor<LancamentoHistorico> captor = ArgumentCaptor.forClass(LancamentoHistorico.class);
        verify(historicoRepository).save(captor.capture());
        assertThat(captor.getValue().getAcao()).isEqualTo(AcaoHistorico.EXCLUIR);
    }

    // ---------- lista ----------

    @Test
    @SuppressWarnings("unchecked")
    void listaTrazOsTresTiposNosTotaisMesmoComZero() {
        Lancamento existente = existente();
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenAnswer(inv -> new PageImpl<>(List.of(existente), inv.getArgument(1), 1));
        when(totais.porTipo(any())).thenReturn(Map.of(TipoLancamento.DESPESA, new Total(1, new BigDecimal("1500.00"))));

        ListaLancamentosResponse r = service.listar(TENANT, LancamentoFiltro.vazio(), -3, 1000);

        assertThat(r.pagina()).isZero();
        assertThat(r.tamanho()).isEqualTo(LancamentoService.TAMANHO_MAXIMO);
        assertThat(r.totais()).extracting(TotalTipoResponse::tipo)
                .containsExactly(TipoLancamento.COMPRA, TipoLancamento.DIVIDA, TipoLancamento.DESPESA);
        assertThat(r.totais()).extracting(TotalTipoResponse::quantidade).containsExactly(0L, 0L, 1L);
        assertThat(r.valorTotal()).isEqualTo(new BigDecimal("1500.00"));
        assertThat(r.itens()).singleElement().satisfies(i -> assertThat(i.clienteNome()).isEqualTo("Padaria Pão Quente"));
    }

    @Test
    void periodoInvertidoEhRecusado() {
        LancamentoFiltro filtro = new LancamentoFiltro(null, null, null, HOJE, HOJE.minusDays(1), null);

        assertThatThrownBy(() -> service.listar(TENANT, filtro, 0, 20))
                .isInstanceOf(BusinessException.class)
                .hasMessage("A data inicial é depois da data final.");
    }

    @Test
    void categoriasVemNaOrdemComOutrosPorUltimo() {
        assertThat(service.categorias()).hasSize(CategoriaLancamento.values().length);
        assertThat(service.categorias().get(service.categorias().size() - 1).rotulo()).isEqualTo("Outros");
    }

    // ---------- comprovante ----------

    @Test
    void anexaComprovantePeloConteudoENaoPeloNome() throws IOException {
        Lancamento existente = existente();
        // Nome diz .pdf, mas o conteúdo é PNG: vale o conteúdo
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "C:\\fotos\\nota fiscal.pdf", "application/pdf",
                PNG);

        LancamentoResponse r = service.anexarComprovante(TENANT, USUARIO, existente.getId(), 0L, arquivo);

        assertThat(r.comprovante().nome()).isEqualTo("nota fiscal.png");
        assertThat(r.comprovante().tipo()).isEqualTo("image/png");
        assertThat(r.comprovante().tamanho()).isEqualTo(PNG.length);
        assertThat(r.editadoEm()).isEqualTo(AGORA);
        String chave = existente.getComprovanteChave();
        assertThat(chave).startsWith(TENANT + "/").endsWith(".png");
        assertThat(Files.readAllBytes(pasta.resolve(chave))).isEqualTo(PNG);
    }

    @Test
    void formatoNaoAceitoNaoGravaNada() throws IOException {
        Lancamento existente = existente();
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "nota.jpg", "image/jpeg",
                "<html>nao sou imagem</html>".getBytes());

        assertThatThrownBy(() -> service.anexarComprovante(TENANT, USUARIO, existente.getId(), 0L, arquivo))
                .isInstanceOf(BusinessException.class)
                .hasMessage(LancamentoService.MSG_FORMATO);
        assertThat(existente.temComprovante()).isFalse();
        try (Stream<Path> arquivos = Files.walk(pasta)) {
            assertThat(arquivos.filter(Files::isRegularFile)).isEmpty();
        }
    }

    @Test
    void arquivoAcimaDoLimiteEhRecusado() {
        Lancamento existente = existente();
        byte[] grande = new byte[2048];
        System.arraycopy(PDF, 0, grande, 0, PDF.length);

        assertThatThrownBy(() -> service.anexarComprovante(TENANT, USUARIO, existente.getId(), 0L,
                new MockMultipartFile("arquivo", "grande.pdf", "application/pdf", grande)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("muito grande");
    }

    @Test
    void trocarComprovanteApagaOAnteriorERemoverTambem() throws IOException {
        Lancamento existente = existente();
        service.anexarComprovante(TENANT, USUARIO, existente.getId(), 0L,
                new MockMultipartFile("arquivo", "a.png", "image/png", PNG));
        String primeira = existente.getComprovanteChave();

        service.anexarComprovante(TENANT, USUARIO, existente.getId(), 0L,
                new MockMultipartFile("arquivo", "b.pdf", "application/pdf", PDF));
        String segunda = existente.getComprovanteChave();

        // Fora de transação (teste de unidade) o anterior é apagado na hora
        assertThat(Files.exists(pasta.resolve(primeira))).isFalse();
        assertThat(Files.exists(pasta.resolve(segunda))).isTrue();

        LancamentoResponse r = service.removerComprovante(TENANT, USUARIO, existente.getId(), 0L);
        assertThat(r.comprovante()).isNull();
        assertThat(Files.exists(pasta.resolve(segunda))).isFalse();
    }

    @Test
    void baixarDevolveOArquivoComNomeETipo() throws IOException {
        Lancamento existente = existente();
        service.anexarComprovante(TENANT, USUARIO, existente.getId(), 0L,
                new MockMultipartFile("arquivo", "recibo.pdf", "application/pdf", PDF));

        LancamentoService.ComprovanteDownload d = service.baixarComprovante(TENANT, existente.getId());
        try (InputStream conteudo = d.conteudo()) {
            assertThat(conteudo.readAllBytes()).isEqualTo(PDF);
        }
        assertThat(d.nome()).isEqualTo("recibo.pdf");
        assertThat(d.contentType()).isEqualTo("application/pdf");
    }

    @Test
    void baixarSemComprovanteEhNaoEncontrado() {
        Lancamento existente = existente();

        assertThatThrownBy(() -> service.baixarComprovante(TENANT, existente.getId()))
                .isInstanceOf(NotFoundException.class)
                .hasMessage(LancamentoService.MSG_SEM_COMPROVANTE);
    }

    // ---------- apoio ----------

    /** Lançamento já gravado: despesa de aluguel de R$ 1.500,00 em 05/10, versão 0. */
    private Lancamento existente() {
        Lancamento l = Lancamento.doEscritorio(TENANT, CLIENTE, USUARIO,
                new DadosLancamento(TipoLancamento.DESPESA, LocalDate.of(2026, 10, 5), "Aluguel de outubro",
                        new BigDecimal("1500.00"), CategoriaLancamento.ALUGUEL),
                Instant.parse("2026-10-05T12:00:00Z"));
        ReflectionTestUtils.setField(l, "id", proximoId++);
        when(repository.findByIdAndTenantIdAndExcluidoEmIsNull(anyLong(), any())).thenAnswer(inv ->
                l.getId().equals(inv.getArgument(0)) && TENANT.equals(inv.getArgument(1)) && !l.isExcluido()
                        ? Optional.of(l)
                        : Optional.empty());
        return l;
    }

    private static LancamentoRequest request(Long clienteId, String descricao, BigDecimal valor, LocalDate data,
            Long versao) {
        return new LancamentoRequest(clienteId, TipoLancamento.DESPESA, data, descricao, valor,
                CategoriaLancamento.ALUGUEL, versao);
    }

    private static Cliente cliente(Long id, boolean ativo) {
        Cliente c = mock(Cliente.class);
        when(c.getId()).thenReturn(id);
        when(c.isAtivo()).thenReturn(ativo);
        when(c.getNome()).thenReturn("Padaria Pão Quente");
        return c;
    }
}
