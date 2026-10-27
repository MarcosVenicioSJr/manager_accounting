package br.com.manageraccounting.modules.dashboard.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import br.com.manageraccounting.modules.cliente.RegimeTributario;
import br.com.manageraccounting.modules.cliente.TipoPessoa;
import br.com.manageraccounting.modules.lancamento.CategoriaLancamento;
import br.com.manageraccounting.modules.lancamento.TipoLancamento;

/**
 * Números da página inicial (Início) do escritório logado.
 *
 * @param carteira     totais de clientes e de acesso ao app
 * @param novosPorMes  clientes cadastrados por mês, do mais antigo ao mês atual (sempre {@code 6} meses, com zeros)
 * @param composicao   clientes ativos por tipo de pessoa e por regime tributário
 * @param semAcessoApp clientes ativos que ainda não têm token do app (os mais recentes primeiro)
 * @param recentes     últimos clientes cadastrados
 * @param lancamentosMes     totais dos lançamentos do mês atual (pela data do lançamento), com o mês anterior
 * @param ultimosLancamentos últimos lançamentos cadastrados, de todos os clientes
 * @param equipe       usuários e convites do escritório; {@code null} quando quem pede não é administrador
 */
public record DashboardResponse(
        Carteira carteira,
        List<CadastrosNoMes> novosPorMes,
        Composicao composicao,
        ClientesSemAcesso semAcessoApp,
        List<ClienteResumo> recentes,
        LancamentosMes lancamentosMes,
        List<LancamentoResumo> ultimosLancamentos,
        Equipe equipe) {

    /**
     * @param novosMesAtual    clientes cadastrados no mês corrente (fuso do escritório)
     * @param novosMesAnterior clientes cadastrados no mês anterior
     */
    public record Carteira(long ativos, long inativos, long comAcessoApp, long semAcessoApp,
            long novosMesAtual, long novosMesAnterior) {
    }

    /** @param mes no formato {@code yyyy-MM} */
    public record CadastrosNoMes(String mes, long quantidade) {
    }

    /**
     * @param porRegime pessoas jurídicas por regime; {@code regime} nulo quer dizer "não informado"
     */
    public record Composicao(long pessoaFisica, long pessoaJuridica, List<RegimeQuantidade> porRegime) {
    }

    public record RegimeQuantidade(RegimeTributario regime, long quantidade) {
    }

    /** @param total quantos são ao todo; {@code itens} traz só os primeiros */
    public record ClientesSemAcesso(long total, List<ClienteResumo> itens) {
    }

    public record ClienteResumo(Long id, String nome, String nomeFantasia, TipoPessoa tipoPessoa, String documento,
            boolean ativo, boolean acessoAppAtivo, Instant dataInclusao) {
    }

    /**
     * @param mes                 mês atual no formato {@code yyyy-MM} (fuso do escritório)
     * @param porTipo             os três tipos, sempre na mesma ordem, mesmo com zero
     * @param valorTotal          soma do mês atual
     * @param valorTotalMesAnterior soma do mês anterior, para comparação
     */
    public record LancamentosMes(String mes, List<TotalTipo> porTipo, long quantidade, BigDecimal valorTotal,
            BigDecimal valorTotalMesAnterior) {
    }

    public record TotalTipo(TipoLancamento tipo, long quantidade, BigDecimal valor) {
    }

    /** @param origem ESCRITORIO ou CLIENTE */
    public record LancamentoResumo(Long id, Long clienteId, String clienteNome, TipoLancamento tipo, LocalDate data,
            String descricao, BigDecimal valor, CategoriaLancamento categoria, String origem, Instant criadoEm) {
    }

    /**
     * @param usuariosAtivos    usuários ativos que já concluíram o primeiro acesso
     * @param convitesPendentes convites enviados, ainda válidos e não aceitos
     */
    public record Equipe(long usuariosAtivos, long convitesPendentes) {
    }
}
