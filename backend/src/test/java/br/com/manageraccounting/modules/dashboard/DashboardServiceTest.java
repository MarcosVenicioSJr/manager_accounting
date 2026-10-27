package br.com.manageraccounting.modules.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
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
import java.time.LocalDate;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.com.manageraccounting.modules.cliente.RegimeTributario;
import br.com.manageraccounting.modules.cliente.TipoPessoa;
import br.com.manageraccounting.modules.dashboard.DashboardConsultas.ContagemCarteira;
import br.com.manageraccounting.modules.dashboard.DashboardConsultas.ContagemComposicao;
import br.com.manageraccounting.modules.dashboard.DashboardConsultas.ContagemEquipe;
import br.com.manageraccounting.modules.dashboard.DashboardConsultas.SomaPorTipo;
import br.com.manageraccounting.modules.dashboard.dto.DashboardResponse;
import br.com.manageraccounting.modules.dashboard.dto.DashboardResponse.CadastrosNoMes;
import br.com.manageraccounting.modules.dashboard.dto.DashboardResponse.ClienteResumo;
import br.com.manageraccounting.modules.dashboard.dto.DashboardResponse.RegimeQuantidade;
import br.com.manageraccounting.modules.dashboard.dto.DashboardResponse.TotalTipo;
import br.com.manageraccounting.modules.lancamento.TipoLancamento;

class DashboardServiceTest {

    private static final Long TENANT = 10L;
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    /** 23h de 31/10 em São Paulo, mas já 01/11 em UTC: o mês atual tem de ser outubro. */
    private static final Instant AGORA = Instant.parse("2026-11-01T02:00:00Z");

    private DashboardConsultas consultas;
    private DashboardService service;

    @BeforeEach
    void setUp() {
        consultas = mock(DashboardConsultas.class);
        service = new DashboardService(consultas, Clock.fixed(AGORA, ZoneOffset.UTC), new DashboardProperties(FUSO));
        when(consultas.carteira(TENANT)).thenReturn(new ContagemCarteira(8, 2, 5));
        when(consultas.cadastrosPorMes(eq(TENANT), any(), eq(FUSO))).thenReturn(Map.of(
                YearMonth.of(2026, 10), 3L,
                YearMonth.of(2026, 9), 1L,
                YearMonth.of(2026, 6), 4L));
        when(consultas.composicao(TENANT)).thenReturn(List.of());
        when(consultas.recentes(eq(TENANT), anyInt())).thenReturn(List.of());
        when(consultas.semAcessoApp(eq(TENANT), anyInt())).thenReturn(List.of(cliente(7L)));
    }

    @Test
    void serieTemSeisMesesAteOMesAtualNoFusoDoEscritorioComZeros() {
        DashboardResponse r = service.montar(TENANT, false);

        assertThat(r.novosPorMes()).extracting(CadastrosNoMes::mes)
                .containsExactly("2026-05", "2026-06", "2026-07", "2026-08", "2026-09", "2026-10");
        assertThat(r.novosPorMes()).extracting(CadastrosNoMes::quantidade).containsExactly(0L, 4L, 0L, 0L, 1L, 3L);
        assertThat(r.carteira().novosMesAtual()).isEqualTo(3);
        assertThat(r.carteira().novosMesAnterior()).isEqualTo(1);
        // A consulta começa à meia-noite de 01/05 em São Paulo (03h UTC)
        verify(consultas).cadastrosPorMes(TENANT, Instant.parse("2026-05-01T03:00:00Z"), FUSO);
    }

    @Test
    void carteiraCalculaQuemEstaSemAcessoAoApp() {
        DashboardResponse r = service.montar(TENANT, false);

        assertThat(r.carteira().ativos()).isEqualTo(8);
        assertThat(r.carteira().inativos()).isEqualTo(2);
        assertThat(r.carteira().comAcessoApp()).isEqualTo(5);
        assertThat(r.carteira().semAcessoApp()).isEqualTo(3);
        assertThat(r.semAcessoApp().total()).isEqualTo(3);
        assertThat(r.semAcessoApp().itens()).extracting(ClienteResumo::id).containsExactly(7L);
        verify(consultas).semAcessoApp(TENANT, DashboardService.LIMITE_LISTAS);
        verify(consultas).recentes(TENANT, DashboardService.LIMITE_LISTAS);
    }

    @Test
    void naoBuscaListaSemAcessoQuandoTodosTemApp() {
        when(consultas.carteira(TENANT)).thenReturn(new ContagemCarteira(4, 0, 4));

        DashboardResponse r = service.montar(TENANT, false);

        assertThat(r.semAcessoApp().total()).isZero();
        assertThat(r.semAcessoApp().itens()).isEmpty();
        verify(consultas, never()).semAcessoApp(anyLong(), anyInt());
    }

    @Test
    void equipeSoParaAdministrador() {
        when(consultas.equipe(TENANT, AGORA)).thenReturn(new ContagemEquipe(3, 1));

        assertThat(service.montar(TENANT, false).equipe()).isNull();
        verify(consultas, never()).equipe(anyLong(), any());

        DashboardResponse admin = service.montar(TENANT, true);
        assertThat(admin.equipe().usuariosAtivos()).isEqualTo(3);
        assertThat(admin.equipe().convitesPendentes()).isEqualTo(1);
    }

    @Test
    void composicaoTrazTodosOsRegimesENaoInformadoSoQuandoExiste() {
        var semNaoInformado = DashboardService.composicao(List.of(
                new ContagemComposicao(TipoPessoa.FISICA, null, 4),
                new ContagemComposicao(TipoPessoa.JURIDICA, RegimeTributario.LUCRO_PRESUMIDO, 2),
                new ContagemComposicao(TipoPessoa.JURIDICA, RegimeTributario.MEI, 1)));

        assertThat(semNaoInformado.pessoaFisica()).isEqualTo(4);
        assertThat(semNaoInformado.pessoaJuridica()).isEqualTo(3);
        assertThat(semNaoInformado.porRegime()).containsExactly(
                new RegimeQuantidade(RegimeTributario.MEI, 1),
                new RegimeQuantidade(RegimeTributario.SIMPLES_NACIONAL, 0),
                new RegimeQuantidade(RegimeTributario.LUCRO_PRESUMIDO, 2),
                new RegimeQuantidade(RegimeTributario.LUCRO_REAL, 0));

        var comNaoInformado = DashboardService.composicao(List.of(
                new ContagemComposicao(TipoPessoa.JURIDICA, null, 2)));
        assertThat(comNaoInformado.porRegime()).last().isEqualTo(new RegimeQuantidade(null, 2));
        assertThat(comNaoInformado.pessoaJuridica()).isEqualTo(2);
    }

    @Test
    void lancamentosDoMesUsamOMesDoFusoETrazemOsTresTipos() {
        // Outubro no fuso do escritório (em UTC já é novembro)
        when(consultas.lancamentosPorTipo(TENANT, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)))
                .thenReturn(List.of(
                        new SomaPorTipo(TipoLancamento.DESPESA, 3, new BigDecimal("150.5")),
                        new SomaPorTipo(TipoLancamento.COMPRA, 1, new BigDecimal("99.90"))));
        when(consultas.lancamentosPorTipo(TENANT, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)))
                .thenReturn(List.of(new SomaPorTipo(TipoLancamento.DIVIDA, 2, new BigDecimal("1000.00"))));

        DashboardResponse r = service.montar(TENANT, false);

        assertThat(r.lancamentosMes().mes()).isEqualTo("2026-10");
        assertThat(r.lancamentosMes().porTipo()).containsExactly(
                new TotalTipo(TipoLancamento.COMPRA, 1, new BigDecimal("99.90")),
                new TotalTipo(TipoLancamento.DIVIDA, 0, new BigDecimal("0.00")),
                new TotalTipo(TipoLancamento.DESPESA, 3, new BigDecimal("150.50")));
        assertThat(r.lancamentosMes().quantidade()).isEqualTo(4);
        assertThat(r.lancamentosMes().valorTotal()).isEqualByComparingTo("250.40");
        assertThat(r.lancamentosMes().valorTotalMesAnterior()).isEqualByComparingTo("1000.00");
        verify(consultas).ultimosLancamentos(TENANT, DashboardService.LIMITE_LISTAS);
    }

    @Test
    void mesSemLancamentosTemZeros() {
        DashboardResponse r = service.montar(TENANT, false);

        assertThat(r.lancamentosMes().quantidade()).isZero();
        assertThat(r.lancamentosMes().valorTotal()).isEqualByComparingTo("0");
        assertThat(r.lancamentosMes().porTipo()).hasSize(3).allMatch(t -> t.quantidade() == 0);
        assertThat(r.ultimosLancamentos()).isEmpty();
    }

    private static ClienteResumo cliente(Long id) {
        return new ClienteResumo(id, "Cliente " + id, null, TipoPessoa.FISICA, "52998224725", true, false,
                Instant.parse("2026-10-20T12:00:00Z"));
    }
}
