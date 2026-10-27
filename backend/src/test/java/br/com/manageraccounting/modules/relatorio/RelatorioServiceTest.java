package br.com.manageraccounting.modules.relatorio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.com.manageraccounting.core.exception.BusinessException;
import br.com.manageraccounting.core.exception.NotFoundException;
import br.com.manageraccounting.modules.cliente.Cliente;
import br.com.manageraccounting.modules.cliente.ClienteRepository;
import br.com.manageraccounting.modules.cliente.TipoPessoa;
import br.com.manageraccounting.modules.lancamento.CategoriaLancamento;
import br.com.manageraccounting.modules.lancamento.OrigemLancamento;
import br.com.manageraccounting.modules.lancamento.TipoLancamento;
import br.com.manageraccounting.modules.relatorio.RelatorioConsultas.LinhaCategoria;
import br.com.manageraccounting.modules.relatorio.RelatorioConsultas.LinhaExtrato;
import br.com.manageraccounting.modules.relatorio.RelatorioConsultas.LinhaResumo;
import br.com.manageraccounting.modules.relatorio.dto.CategoriasResponse;
import br.com.manageraccounting.modules.relatorio.dto.ExtratoClienteResponse;
import br.com.manageraccounting.modules.relatorio.dto.ExtratoClienteResponse.TotalCategoria;
import br.com.manageraccounting.modules.relatorio.dto.ExtratoClienteResponse.TotalTipo;
import br.com.manageraccounting.modules.relatorio.dto.ResumoEscritorioResponse;
import br.com.manageraccounting.modules.relatorio.dto.ResumoEscritorioResponse.Linha;
import br.com.manageraccounting.modules.tenant.Tenant;
import br.com.manageraccounting.modules.tenant.TenantRepository;

class RelatorioServiceTest {

    private static final Instant AGORA = Instant.parse("2026-10-27T00:30:00Z");
    private static final Long TENANT = 10L;
    private static final LocalDate DE = LocalDate.of(2026, 10, 1);
    private static final LocalDate ATE = LocalDate.of(2026, 10, 31);

    private RelatorioConsultas consultas;
    private ClienteRepository clienteRepository;
    private RelatorioService service;

    @BeforeEach
    void setUp() {
        consultas = mock(RelatorioConsultas.class);
        clienteRepository = mock(ClienteRepository.class);
        TenantRepository tenantRepository = mock(TenantRepository.class);
        Tenant tenant = mock(Tenant.class);
        when(tenant.getNomeEmpresa()).thenReturn("ILLUM Contabilidade");
        when(tenantRepository.findById(TENANT)).thenReturn(Optional.of(tenant));
        service = new RelatorioService(consultas, clienteRepository, tenantRepository,
                Clock.fixed(AGORA, ZoneOffset.UTC));

        Cliente cliente = mock(Cliente.class);
        when(cliente.getId()).thenReturn(100L);
        when(cliente.getNome()).thenReturn("Padaria Pão Quente");
        when(cliente.getTipoPessoa()).thenReturn(TipoPessoa.JURIDICA);
        when(cliente.getDocumento()).thenReturn("11222333000181");
        when(cliente.isAtivo()).thenReturn(false);
        when(clienteRepository.findByIdAndTenantId(100L, TENANT)).thenReturn(Optional.of(cliente));
    }

    @Test
    void extratoSomaPorTipoEPorCategoria() {
        when(consultas.extrato(TENANT, 100L, DE, ATE, RelatorioService.LIMITE_EXTRATO + 1)).thenReturn(List.of(
                linha(1L, TipoLancamento.DESPESA, CategoriaLancamento.ALUGUEL, "1500"),
                linha(2L, TipoLancamento.COMPRA, CategoriaLancamento.ALIMENTACAO, "320.4"),
                linha(3L, TipoLancamento.DESPESA, CategoriaLancamento.ALIMENTACAO, "79.60")));

        ExtratoClienteResponse r = service.extrato(TENANT, 100L, DE, ATE);

        assertThat(r.escritorio()).isEqualTo("ILLUM Contabilidade");
        assertThat(r.cliente().nome()).isEqualTo("Padaria Pão Quente");
        assertThat(r.cliente().ativo()).isFalse();
        assertThat(r.geradoEm()).isEqualTo(AGORA);
        assertThat(r.quantidade()).isEqualTo(3);
        assertThat(r.valorTotal()).isEqualTo(new BigDecimal("1900.00"));
        assertThat(r.itens().get(1).valor()).isEqualTo(new BigDecimal("320.40"));
        assertThat(r.itens().get(0).categoriaRotulo()).isEqualTo("Aluguel");
        assertThat(r.totaisPorTipo()).containsExactly(
                new TotalTipo(TipoLancamento.COMPRA, 1, new BigDecimal("320.40")),
                new TotalTipo(TipoLancamento.DIVIDA, 0, new BigDecimal("0.00")),
                new TotalTipo(TipoLancamento.DESPESA, 2, new BigDecimal("1579.60")));
        assertThat(r.totaisPorCategoria()).containsExactly(
                new TotalCategoria(CategoriaLancamento.ALUGUEL, "Aluguel", 1, new BigDecimal("1500.00")),
                new TotalCategoria(CategoriaLancamento.ALIMENTACAO, "Alimentação", 2, new BigDecimal("400.00")));
    }

    @Test
    void extratoDeClienteDeOutroEscritorioNaoEhEncontrado() {
        when(clienteRepository.findByIdAndTenantId(999L, TENANT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.extrato(TENANT, 999L, DE, ATE))
                .isInstanceOf(NotFoundException.class)
                .hasMessage(RelatorioService.MSG_CLIENTE_NAO_ENCONTRADO);
        verify(consultas, never()).extrato(anyLong(), anyLong(), any(), any(), anyInt());
    }

    @Test
    void extratoAcimaDoLimiteEhRecusado() {
        LinhaExtrato l = linha(1L, TipoLancamento.COMPRA, CategoriaLancamento.OUTROS, "1");
        when(consultas.extrato(eq(TENANT), eq(100L), any(), any(), anyInt()))
                .thenReturn(Collections.nCopies(RelatorioService.LIMITE_EXTRATO + 1, l));

        assertThatThrownBy(() -> service.extrato(TENANT, 100L, DE, ATE))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("mais de 5.000");
    }

    @Test
    void periodoObrigatorioEDeNoMaximoUmAno() {
        assertThatThrownBy(() -> service.resumo(TENANT, null, ATE)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.resumo(TENANT, ATE, DE))
                .hasMessage("A data inicial é depois da data final.");
        assertThatThrownBy(() -> service.resumo(TENANT, LocalDate.of(2025, 1, 1), LocalDate.of(2026, 1, 2)))
                .hasMessage("O período pode ter no máximo um ano.");
        // 366 dias (ano bissexto inteiro) passa
        RelatorioService.validarPeriodo(LocalDate.of(2028, 1, 1), LocalDate.of(2028, 12, 31));
        assertThatThrownBy(() -> service.extrato(TENANT, null, DE, ATE)).hasMessage("Escolha o cliente.");
    }

    @Test
    void resumoOrdenaPorTotalESomaTudo() {
        when(consultas.resumo(TENANT, DE, ATE)).thenReturn(List.of(
                resumo(1L, "Zeca", 0, "0", "0", "0"),
                resumo(2L, "Maria", 3, "330.4", "0", "1500"),
                resumo(3L, "Padaria", 2, "100.10", "5000", "0"),
                resumo(4L, "Ana", 0, "0", "0", "0")));

        ResumoEscritorioResponse r = service.resumo(TENANT, DE, ATE);

        assertThat(r.linhas()).extracting(Linha::nome).containsExactly("Padaria", "Maria", "Ana", "Zeca");
        assertThat(r.linhas().get(1).total()).isEqualTo(new BigDecimal("1830.40"));
        assertThat(r.totais().quantidade()).isEqualTo(5);
        assertThat(r.totais().compras()).isEqualTo(new BigDecimal("430.50"));
        assertThat(r.totais().dividas()).isEqualTo(new BigDecimal("5000.00"));
        assertThat(r.totais().total()).isEqualTo(new BigDecimal("6930.50"));
        assertThat(r.clientesComMovimento()).isEqualTo(2);
        assertThat(r.clientesSemMovimento()).isEqualTo(2);
    }

    @Test
    void porCategoriaDeTodosOrdenaECalculaPercentual() {
        when(consultas.porCategoria(TENANT, null, DE, ATE)).thenReturn(List.of(
                new LinhaCategoria(CategoriaLancamento.ALIMENTACAO, 2, 1, new BigDecimal("100"),
                        new BigDecimal("50.5"), BigDecimal.ZERO),
                new LinhaCategoria(CategoriaLancamento.ALUGUEL, 2, 2, BigDecimal.ZERO, BigDecimal.ZERO,
                        new BigDecimal("2000"))));
        when(consultas.clientesComLancamento(TENANT, null, DE, ATE)).thenReturn(2L);

        CategoriasResponse r = service.porCategoria(TENANT, null, DE, ATE);

        assertThat(r.cliente()).isNull();
        assertThat(r.linhas()).extracting(CategoriasResponse.Linha::rotulo).containsExactly("Aluguel", "Alimentação");
        assertThat(r.linhas().get(0).percentual()).isEqualTo(new BigDecimal("93.0"));
        assertThat(r.linhas().get(1).percentual()).isEqualTo(new BigDecimal("7.0"));
        assertThat(r.linhas().get(1).total()).isEqualTo(new BigDecimal("150.50"));
        assertThat(r.totais().total()).isEqualTo(new BigDecimal("2150.50"));
        assertThat(r.totais().quantidade()).isEqualTo(4);
        assertThat(r.clientes()).isEqualTo(2);
    }

    @Test
    void porCategoriaDeUmClienteValidaOEscritorio() {
        when(consultas.porCategoria(TENANT, 100L, DE, ATE)).thenReturn(List.of());

        CategoriasResponse r = service.porCategoria(TENANT, 100L, DE, ATE);

        assertThat(r.cliente().nome()).isEqualTo("Padaria Pão Quente");
        assertThat(r.linhas()).isEmpty();
        assertThat(r.totais().total()).isEqualTo(new BigDecimal("0.00"));
        assertThat(r.clientes()).isZero();
        verify(consultas, never()).clientesComLancamento(anyLong(), any(), any(), any());

        when(clienteRepository.findByIdAndTenantId(999L, TENANT)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.porCategoria(TENANT, 999L, DE, ATE)).isInstanceOf(NotFoundException.class);
    }

    private static LinhaExtrato linha(Long id, TipoLancamento tipo, CategoriaLancamento categoria, String valor) {
        return new LinhaExtrato(id, LocalDate.of(2026, 10, id.intValue()), tipo, "Lançamento " + id, categoria,
                new BigDecimal(valor), OrigemLancamento.ESCRITORIO, false);
    }

    private static LinhaResumo resumo(Long id, String nome, long qtd, String compras, String dividas,
            String despesas) {
        return new LinhaResumo(id, nome, TipoPessoa.FISICA, "52998224725", true, qtd, new BigDecimal(compras),
                new BigDecimal(dividas), new BigDecimal(despesas));
    }
}
