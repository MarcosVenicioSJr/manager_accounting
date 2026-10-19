package br.com.manageraccounting.modules.cliente;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import br.com.manageraccounting.core.exception.BusinessException;
import br.com.manageraccounting.core.exception.ConflictException;
import br.com.manageraccounting.core.exception.NotFoundException;
import br.com.manageraccounting.modules.cliente.dto.ClienteListaItem;
import br.com.manageraccounting.modules.cliente.dto.ClienteRequest;
import br.com.manageraccounting.modules.cliente.dto.ClienteResponse;
import br.com.manageraccounting.modules.cliente.dto.PaginaResponse;
import br.com.manageraccounting.modules.cliente.dto.TokenGeradoResponse;

class ClienteServiceTest {

    private static final Instant AGORA = Instant.parse("2026-10-19T12:00:00Z");
    private static final Long TENANT = 10L;
    private static final Long OUTRO_TENANT = 20L;
    private static final Long USUARIO = 1L;
    private static final String CNPJ = "11222333000181";

    private ClienteRepository clienteRepository;
    private ClienteTokenAcessoRepository tokenRepository;
    private ClienteService service;

    @BeforeEach
    void setUp() {
        clienteRepository = mock(ClienteRepository.class);
        tokenRepository = mock(ClienteTokenAcessoRepository.class);
        service = new ClienteService(clienteRepository, tokenRepository, Clock.fixed(AGORA, ZoneOffset.UTC));
        when(tokenRepository.findByClienteIdAndRevogadoEmIsNull(anyLong())).thenReturn(Optional.empty());
    }

    // ---------- cadastro ----------

    @Test
    void criarUsaOTenantDoTokenENormalizaOsDados() {
        ClienteResponse criado = service.criar(TENANT, new ClienteRequest(TipoPessoa.JURIDICA,
                "11.222.333/0001-81", "  Padaria Pão Quente LTDA ", " Pão Quente ", " Contato@PaoQuente.com.br ",
                "(85) 98888-7777", RegimeTributario.SIMPLES_NACIONAL, "   ", null));

        ArgumentCaptor<Cliente> captor = ArgumentCaptor.forClass(Cliente.class);
        verify(clienteRepository).saveAndFlush(captor.capture());
        Cliente salvo = captor.getValue();
        assertThat(salvo.getTenantId()).isEqualTo(TENANT);
        assertThat(salvo.getDocumento()).isEqualTo(CNPJ);
        assertThat(salvo.getNome()).isEqualTo("Padaria Pão Quente LTDA");
        assertThat(salvo.getNomeFantasia()).isEqualTo("Pão Quente");
        assertThat(salvo.getEmail()).isEqualTo("contato@paoquente.com.br");
        assertThat(salvo.getTelefone()).isEqualTo("85988887777");
        assertThat(salvo.getObservacoes()).isNull();
        assertThat(salvo.isAtivo()).isTrue();
        assertThat(salvo.getDataInclusao()).isEqualTo(AGORA);
        assertThat(criado.acessoApp().ativo()).isFalse();
    }

    @Test
    void pessoaFisicaDescartaNomeFantasiaERegime() {
        service.criar(TENANT, new ClienteRequest(TipoPessoa.FISICA, "529.982.247-25", "Maria Souza",
                "Fantasia", null, null, RegimeTributario.MEI, null, null));

        ArgumentCaptor<Cliente> captor = ArgumentCaptor.forClass(Cliente.class);
        verify(clienteRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getNomeFantasia()).isNull();
        assertThat(captor.getValue().getRegimeTributario()).isNull();
    }

    @Test
    void criarComDocumentoJaCadastradoNoTenant() {
        when(clienteRepository.existsByTenantIdAndDocumento(TENANT, CNPJ)).thenReturn(true);

        assertThatThrownBy(() -> service.criar(TENANT, requestPj(null)))
                .isInstanceOf(ConflictException.class)
                .hasMessage(ClienteService.MSG_DUPLICADO)
                .extracting("codigo").isEqualTo(ClienteService.CODIGO_DUPLICADO);
        verify(clienteRepository, never()).saveAndFlush(any());
    }

    @Test
    void cadastroSimultaneoDoMesmoDocumentoViraConflito() {
        when(clienteRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("uk"));

        assertThatThrownBy(() -> service.criar(TENANT, requestPj(null)))
                .isInstanceOf(ConflictException.class)
                .hasMessage(ClienteService.MSG_DUPLICADO)
                .extracting("codigo").isEqualTo(ClienteService.CODIGO_DUPLICADO);
    }

    @Test
    void telefoneSemDdd() {
        ClienteRequest request = new ClienteRequest(TipoPessoa.JURIDICA, CNPJ, "Empresa", null, null,
                "98888-777", null, null, null);

        assertThatThrownBy(() -> service.criar(TENANT, request)).isInstanceOf(BusinessException.class)
                .hasMessageContaining("Telefone inválido");
    }

    @Test
    void nomeSoComEspacos() {
        ClienteRequest request = new ClienteRequest(TipoPessoa.JURIDICA, CNPJ, "   ", null, null, null, null,
                null, null);

        assertThatThrownBy(() -> service.criar(TENANT, request)).isInstanceOf(BusinessException.class);
    }

    @Test
    void documentoInvalido() {
        ClienteRequest request = new ClienteRequest(TipoPessoa.JURIDICA, "11222333000182", "Empresa", null, null,
                null, null, null, null);

        assertThatThrownBy(() -> service.criar(TENANT, request)).isInstanceOf(BusinessException.class)
                .hasMessage("CNPJ inválido.");
    }

    // ---------- isolamento por tenant ----------

    @Test
    void clienteDeOutroTenantRespondeNaoEncontrado() {
        when(clienteRepository.findByIdAndTenantId(5L, TENANT)).thenReturn(Optional.of(cliente(5L, TENANT, true, 0)));
        when(clienteRepository.findByIdAndTenantId(5L, OUTRO_TENANT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obter(OUTRO_TENANT, 5L)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.atualizar(OUTRO_TENANT, 5L, requestPj(0L)))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.desativar(OUTRO_TENANT, USUARIO, 5L)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.reativar(OUTRO_TENANT, 5L)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.revogarToken(OUTRO_TENANT, USUARIO, 5L))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.gerarToken(OUTRO_TENANT, USUARIO, 5L))
                .isInstanceOf(NotFoundException.class);

        verify(clienteRepository, never()).saveAndFlush(any());
        verify(tokenRepository, never()).revogarAtivos(any(), any());
        verify(tokenRepository, never()).saveAndFlush(any());
    }

    // ---------- edição ----------

    @Test
    void edicaoSemVersao() {
        assertThatThrownBy(() -> service.atualizar(TENANT, 5L, requestPj(null)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void edicaoComVersaoDesatualizada() {
        when(clienteRepository.findByIdAndTenantId(5L, TENANT)).thenReturn(Optional.of(cliente(5L, TENANT, true, 3)));

        assertThatThrownBy(() -> service.atualizar(TENANT, 5L, requestPj(2L)))
                .isInstanceOf(ConflictException.class)
                .hasMessage(ClienteService.MSG_EDICAO_SIMULTANEA)
                .extracting("codigo").isEqualTo(ClienteService.CODIGO_EDICAO_SIMULTANEA);
        verify(clienteRepository, never()).saveAndFlush(any());
    }

    @Test
    void edicaoQueColideNoBancoComOutraEdicao() {
        when(clienteRepository.findByIdAndTenantId(5L, TENANT)).thenReturn(Optional.of(cliente(5L, TENANT, true, 3)));
        when(clienteRepository.saveAndFlush(any())).thenThrow(new OptimisticLockingFailureException("versao"));

        assertThatThrownBy(() -> service.atualizar(TENANT, 5L, requestPj(3L)))
                .isInstanceOf(ConflictException.class)
                .hasMessage(ClienteService.MSG_EDICAO_SIMULTANEA)
                .extracting("codigo").isEqualTo(ClienteService.CODIGO_EDICAO_SIMULTANEA);
    }

    @Test
    void edicaoComDocumentoDeOutroCliente() {
        when(clienteRepository.findByIdAndTenantId(5L, TENANT)).thenReturn(Optional.of(cliente(5L, TENANT, true, 3)));
        when(clienteRepository.existsByTenantIdAndDocumentoAndIdNot(TENANT, CNPJ, 5L)).thenReturn(true);

        assertThatThrownBy(() -> service.atualizar(TENANT, 5L, requestPj(3L)))
                .isInstanceOf(ConflictException.class)
                .hasMessage(ClienteService.MSG_DUPLICADO)
                .extracting("codigo").isEqualTo(ClienteService.CODIGO_DUPLICADO);
    }

    @Test
    void edicaoAtualizaDadosEData() {
        Cliente cliente = cliente(5L, TENANT, true, 3);
        when(clienteRepository.findByIdAndTenantId(5L, TENANT)).thenReturn(Optional.of(cliente));

        ClienteResponse resposta = service.atualizar(TENANT, 5L, new ClienteRequest(TipoPessoa.JURIDICA, CNPJ,
                "Nome Novo", null, null, null, RegimeTributario.LUCRO_REAL, "obs", 3L));

        assertThat(resposta.nome()).isEqualTo("Nome Novo");
        assertThat(resposta.regimeTributario()).isEqualTo(RegimeTributario.LUCRO_REAL);
        assertThat(cliente.getDataAtualizacao()).isEqualTo(AGORA);
        assertThat(cliente.getTenantId()).isEqualTo(TENANT);
        verify(clienteRepository).saveAndFlush(cliente);
    }

    // ---------- desativar / reativar ----------

    @Test
    void desativarRevogaOTokenDoApp() {
        Cliente cliente = cliente(5L, TENANT, true, 0);
        when(clienteRepository.findByIdAndTenantId(5L, TENANT)).thenReturn(Optional.of(cliente));

        service.desativar(TENANT, USUARIO, 5L);

        assertThat(cliente.isAtivo()).isFalse();
        verify(clienteRepository).saveAndFlush(cliente);
        verify(tokenRepository).revogarAtivos(5L, AGORA);
    }

    @Test
    void desativarClienteJaInativoNaoMudaNada() {
        when(clienteRepository.findByIdAndTenantId(5L, TENANT)).thenReturn(Optional.of(cliente(5L, TENANT, false, 0)));

        service.desativar(TENANT, USUARIO, 5L);

        verify(clienteRepository, never()).saveAndFlush(any());
        verify(tokenRepository, never()).revogarAtivos(any(), any());
    }

    @Test
    void reativar() {
        Cliente cliente = cliente(5L, TENANT, false, 0);
        when(clienteRepository.findByIdAndTenantId(5L, TENANT)).thenReturn(Optional.of(cliente));

        service.reativar(TENANT, 5L);

        assertThat(cliente.isAtivo()).isTrue();
        verify(clienteRepository).saveAndFlush(cliente);
    }

    // ---------- token do app ----------

    @Test
    void gerarTokenRevogaOAnteriorEGuardaSoOHash() {
        when(clienteRepository.findForUpdate(5L, TENANT)).thenReturn(Optional.of(cliente(5L, TENANT, true, 0)));

        TokenGeradoResponse resposta = service.gerarToken(TENANT, USUARIO, 5L);

        assertThat(resposta.token()).matches("[0-9A-Z]{4}(-[0-9A-Z]{4}){3}");
        assertThat(resposta.geradoEm()).isEqualTo(AGORA);
        verify(tokenRepository).revogarAtivos(5L, AGORA);

        ArgumentCaptor<ClienteTokenAcesso> captor = ArgumentCaptor.forClass(ClienteTokenAcesso.class);
        verify(tokenRepository).saveAndFlush(captor.capture());
        ClienteTokenAcesso salvo = captor.getValue();
        assertThat(salvo.getClienteId()).isEqualTo(5L);
        assertThat(salvo.getCriadoPor()).isEqualTo(USUARIO);
        assertThat(salvo.getTokenHash()).isEqualTo(TokenAcessoApp.hash(resposta.token()))
                .doesNotContain(TokenAcessoApp.normalizar(resposta.token()));
    }

    @Test
    void naoGeraTokenParaClienteInativo() {
        when(clienteRepository.findForUpdate(5L, TENANT)).thenReturn(Optional.of(cliente(5L, TENANT, false, 0)));

        assertThatThrownBy(() -> service.gerarToken(TENANT, USUARIO, 5L)).isInstanceOf(BusinessException.class);
        verify(tokenRepository, never()).saveAndFlush(any());
    }

    @Test
    void revogarToken() {
        when(clienteRepository.findByIdAndTenantId(5L, TENANT)).thenReturn(Optional.of(cliente(5L, TENANT, true, 0)));

        service.revogarToken(TENANT, USUARIO, 5L);

        verify(tokenRepository).revogarAtivos(5L, AGORA);
    }

    // ---------- lista ----------

    @Test
    @SuppressWarnings("unchecked")
    void listaLimitaOTamanhoEMarcaQuemTemAcessoAoApp() {
        Cliente comToken = cliente(1L, TENANT, true, 0);
        Cliente semToken = cliente(2L, TENANT, true, 0);
        when(clienteRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(comToken, semToken), PageRequest.of(0, 100), 2));
        when(tokenRepository.findClienteIdsComTokenAtivo(List.of(1L, 2L))).thenReturn(List.of(1L));

        PaginaResponse<ClienteListaItem> pagina = service.listar(TENANT, null, StatusFiltro.ATIVOS, -3, 5000);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(clienteRepository).findAll(any(Specification.class), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isZero();
        assertThat(captor.getValue().getPageSize()).isEqualTo(ClienteService.TAMANHO_MAXIMO);

        assertThat(pagina.total()).isEqualTo(2);
        assertThat(pagina.itens()).extracting(ClienteListaItem::acessoAppAtivo).containsExactly(true, false);
    }

    @Test
    @SuppressWarnings("unchecked")
    void listaVaziaNaoConsultaTokens() {
        when(clienteRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        PaginaResponse<ClienteListaItem> pagina = service.listar(TENANT, "x", StatusFiltro.TODOS, 0, 0);

        assertThat(pagina.itens()).isEmpty();
        verify(tokenRepository, never()).findClienteIdsComTokenAtivo(any());
    }

    // ---------- apoio ----------

    private static ClienteRequest requestPj(Long versao) {
        return new ClienteRequest(TipoPessoa.JURIDICA, CNPJ, "Empresa Exemplo LTDA", null, null, null, null, null,
                versao);
    }

    private static Cliente cliente(Long id, Long tenantId, boolean ativo, long versao) {
        Cliente cliente = BeanUtils.instantiateClass(Cliente.class);
        ReflectionTestUtils.setField(cliente, "id", id);
        ReflectionTestUtils.setField(cliente, "tenantId", tenantId);
        ReflectionTestUtils.setField(cliente, "tipoPessoa", TipoPessoa.JURIDICA);
        ReflectionTestUtils.setField(cliente, "documento", CNPJ);
        ReflectionTestUtils.setField(cliente, "nome", "Empresa " + id);
        ReflectionTestUtils.setField(cliente, "ativo", ativo);
        ReflectionTestUtils.setField(cliente, "versao", versao);
        return cliente;
    }
}
