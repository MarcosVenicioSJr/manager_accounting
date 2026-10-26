package br.com.manageraccounting.modules.dashboard;

import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.manageraccounting.modules.cliente.RegimeTributario;
import br.com.manageraccounting.modules.cliente.TipoPessoa;
import br.com.manageraccounting.modules.dashboard.DashboardConsultas.ContagemCarteira;
import br.com.manageraccounting.modules.dashboard.DashboardConsultas.ContagemComposicao;
import br.com.manageraccounting.modules.dashboard.DashboardConsultas.ContagemEquipe;
import br.com.manageraccounting.modules.dashboard.dto.DashboardResponse;
import br.com.manageraccounting.modules.dashboard.dto.DashboardResponse.CadastrosNoMes;
import br.com.manageraccounting.modules.dashboard.dto.DashboardResponse.Carteira;
import br.com.manageraccounting.modules.dashboard.dto.DashboardResponse.ClientesSemAcesso;
import br.com.manageraccounting.modules.dashboard.dto.DashboardResponse.Composicao;
import br.com.manageraccounting.modules.dashboard.dto.DashboardResponse.Equipe;
import br.com.manageraccounting.modules.dashboard.dto.DashboardResponse.RegimeQuantidade;

/**
 * Monta a página inicial do escritório com os dados que já existem (clientes, acesso ao app e equipe).
 * Os números de lançamentos entram aqui quando o módulo de lançamentos existir.
 */
@Service
public class DashboardService {

    /** Meses no gráfico de novos clientes, contando o atual. */
    static final int MESES_SERIE = 6;
    /** Itens nas listas "sem acesso ao app" e "cadastrados recentemente". */
    static final int LIMITE_LISTAS = 5;

    private final DashboardConsultas consultas;
    private final Clock clock;
    private final ZoneId fuso;

    public DashboardService(DashboardConsultas consultas, Clock clock, DashboardProperties properties) {
        this.consultas = consultas;
        this.clock = clock;
        this.fuso = properties.fusoHorario();
    }

    /**
     * @param admin quem pede é administrador do escritório (só ele recebe o bloco da equipe)
     */
    @Transactional(readOnly = true)
    public DashboardResponse montar(Long tenantId, boolean admin) {
        Instant agora = clock.instant();
        YearMonth mesAtual = YearMonth.from(agora.atZone(fuso));
        YearMonth primeiroMes = mesAtual.minusMonths(MESES_SERIE - 1L);
        Instant desde = primeiroMes.atDay(1).atStartOfDay(fuso).toInstant();

        List<CadastrosNoMes> serie = serieMensal(consultas.cadastrosPorMes(tenantId, desde, fuso), primeiroMes);
        long novosMesAtual = serie.get(serie.size() - 1).quantidade();
        long novosMesAnterior = serie.get(serie.size() - 2).quantidade();

        ContagemCarteira contagem = consultas.carteira(tenantId);
        long semAcesso = Math.max(0, contagem.ativos() - contagem.comAcessoApp());
        Carteira carteira = new Carteira(contagem.ativos(), contagem.inativos(), contagem.comAcessoApp(), semAcesso,
                novosMesAtual, novosMesAnterior);

        ClientesSemAcesso semAcessoApp = new ClientesSemAcesso(semAcesso,
                semAcesso == 0 ? List.of() : consultas.semAcessoApp(tenantId, LIMITE_LISTAS));

        Equipe equipe = null;
        if (admin) {
            ContagemEquipe e = consultas.equipe(tenantId, agora);
            equipe = new Equipe(e.usuariosAtivos(), e.convitesPendentes());
        }

        return new DashboardResponse(carteira, serie, composicao(consultas.composicao(tenantId)), semAcessoApp,
                consultas.recentes(tenantId, LIMITE_LISTAS), equipe);
    }

    /** Completa com zero os meses sem cadastro, do primeiro mês até o atual. */
    static List<CadastrosNoMes> serieMensal(Map<YearMonth, Long> porMes, YearMonth primeiroMes) {
        List<CadastrosNoMes> serie = new ArrayList<>(MESES_SERIE);
        for (int i = 0; i < MESES_SERIE; i++) {
            YearMonth mes = primeiroMes.plusMonths(i);
            serie.add(new CadastrosNoMes(mes.toString(), porMes.getOrDefault(mes, 0L)));
        }
        return serie;
    }

    /**
     * Todos os regimes aparecem, mesmo com zero, na ordem do enum; "não informado" (regime nulo) só aparece
     * quando existe pessoa jurídica sem regime.
     */
    static Composicao composicao(List<ContagemComposicao> contagens) {
        long pf = 0;
        long pj = 0;
        long[] porRegime = new long[RegimeTributario.values().length];
        long semRegime = 0;
        for (ContagemComposicao c : contagens) {
            if (c.tipoPessoa() == TipoPessoa.FISICA) {
                pf += c.quantidade();
                continue;
            }
            pj += c.quantidade();
            if (c.regime() == null) {
                semRegime += c.quantidade();
            } else {
                porRegime[c.regime().ordinal()] += c.quantidade();
            }
        }
        List<RegimeQuantidade> regimes = new ArrayList<>();
        for (RegimeTributario r : RegimeTributario.values()) {
            regimes.add(new RegimeQuantidade(r, porRegime[r.ordinal()]));
        }
        if (semRegime > 0) {
            regimes.add(new RegimeQuantidade(null, semRegime));
        }
        return new Composicao(pf, pj, regimes);
    }
}
