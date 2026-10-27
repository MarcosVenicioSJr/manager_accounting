package br.com.manageraccounting.modules.dashboard;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import br.com.manageraccounting.modules.cliente.RegimeTributario;
import br.com.manageraccounting.modules.cliente.TipoPessoa;
import br.com.manageraccounting.modules.dashboard.dto.DashboardResponse.ClienteResumo;
import br.com.manageraccounting.modules.dashboard.dto.DashboardResponse.LancamentoResumo;
import br.com.manageraccounting.modules.lancamento.CategoriaLancamento;
import br.com.manageraccounting.modules.lancamento.TipoLancamento;

/**
 * Consultas de agregação do dashboard, em SQL direto (contagens e agrupamentos por mês ficam bem mais simples
 * assim do que em JPQL). Toda consulta filtra pelo {@code tenant_id} recebido, que vem sempre do token.
 */
@Repository
public class DashboardConsultas {

    /** Token do app ativo do cliente {@code c}. */
    private static final String TEM_TOKEN_ATIVO = """
            exists (select 1 from cliente_token_acesso t where t.cliente_id = c.id and t.revogado_em is null)""";

    private static final String COLUNAS_CLIENTE = """
            c.id, c.nome, c.nome_fantasia, c.tipo_pessoa, c.documento, c.ativo, c.data_inclusao,
            """ + TEM_TOKEN_ATIVO + " as acesso_app_ativo";

    private final NamedParameterJdbcTemplate jdbc;

    public DashboardConsultas(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record ContagemCarteira(long ativos, long inativos, long comAcessoApp) {
    }

    public record ContagemComposicao(TipoPessoa tipoPessoa, RegimeTributario regime, long quantidade) {
    }

    public record ContagemEquipe(long usuariosAtivos, long convitesPendentes) {
    }

    public record SomaPorTipo(TipoLancamento tipo, long quantidade, BigDecimal valor) {
    }

    public ContagemCarteira carteira(Long tenantId) {
        String sql = """
                select count(*) filter (where c.ativo) as ativos,
                       count(*) filter (where not c.ativo) as inativos,
                       count(*) filter (where c.ativo and %s) as com_acesso
                from cliente c
                where c.tenant_id = :tenantId
                """.formatted(TEM_TOKEN_ATIVO);
        return jdbc.queryForObject(sql, tenant(tenantId), (rs, i) -> new ContagemCarteira(
                rs.getLong("ativos"), rs.getLong("inativos"), rs.getLong("com_acesso")));
    }

    /**
     * Clientes cadastrados (ativos ou não) por mês, a partir de {@code desde}, com o mês calculado no fuso
     * informado. Meses sem cadastro não aparecem no mapa.
     */
    public Map<YearMonth, Long> cadastrosPorMes(Long tenantId, Instant desde, ZoneId fuso) {
        String sql = """
                select to_char(date_trunc('month', c.data_inclusao at time zone :fuso), 'YYYY-MM') as mes,
                       count(*) as quantidade
                from cliente c
                where c.tenant_id = :tenantId and c.data_inclusao >= :desde
                group by 1
                order by 1
                """;
        MapSqlParameterSource params = tenant(tenantId)
                .addValue("desde", Timestamp.from(desde))
                .addValue("fuso", fuso.getId());
        Map<YearMonth, Long> resultado = new LinkedHashMap<>();
        jdbc.query(sql, params, rs -> {
            resultado.put(YearMonth.parse(rs.getString("mes")), rs.getLong("quantidade"));
        });
        return resultado;
    }

    /** Clientes ativos agrupados por tipo de pessoa e regime tributário. */
    public List<ContagemComposicao> composicao(Long tenantId) {
        String sql = """
                select c.tipo_pessoa, c.regime_tributario, count(*) as quantidade
                from cliente c
                where c.tenant_id = :tenantId and c.ativo
                group by c.tipo_pessoa, c.regime_tributario
                """;
        return jdbc.query(sql, tenant(tenantId), (rs, i) -> {
            String regime = rs.getString("regime_tributario");
            return new ContagemComposicao(
                    TipoPessoa.valueOf(rs.getString("tipo_pessoa")),
                    regime == null ? null : RegimeTributario.valueOf(regime),
                    rs.getLong("quantidade"));
        });
    }

    /** Clientes ativos sem token do app, do cadastro mais recente para o mais antigo. */
    public List<ClienteResumo> semAcessoApp(Long tenantId, int limite) {
        String sql = """
                select %s
                from cliente c
                where c.tenant_id = :tenantId and c.ativo and not %s
                order by c.data_inclusao desc, c.id desc
                limit :limite
                """.formatted(COLUNAS_CLIENTE, TEM_TOKEN_ATIVO);
        return jdbc.query(sql, tenant(tenantId).addValue("limite", limite), DashboardConsultas::cliente);
    }

    /** Últimos clientes cadastrados, ativos ou não. */
    public List<ClienteResumo> recentes(Long tenantId, int limite) {
        String sql = """
                select %s
                from cliente c
                where c.tenant_id = :tenantId
                order by c.data_inclusao desc, c.id desc
                limit :limite
                """.formatted(COLUNAS_CLIENTE);
        return jdbc.query(sql, tenant(tenantId).addValue("limite", limite), DashboardConsultas::cliente);
    }

    public ContagemEquipe equipe(Long tenantId, Instant agora) {
        String sql = """
                select
                  (select count(*) from usuario u
                    where u.tenant_id = :tenantId and u.ativo and u.primeiro_acesso_concluido) as usuarios_ativos,
                  (select count(distinct cu.usuario_id) from convite_usuario cu
                    join usuario u on u.id = cu.usuario_id
                    where u.tenant_id = :tenantId and u.ativo
                      and cu.aceito_em is null and cu.invalidado_em is null and cu.expira_em > :agora)
                    as convites_pendentes
                """;
        return jdbc.queryForObject(sql, tenant(tenantId).addValue("agora", Timestamp.from(agora)),
                (rs, i) -> new ContagemEquipe(rs.getLong("usuarios_ativos"), rs.getLong("convites_pendentes")));
    }

    /** Lançamentos não excluídos com data entre {@code de} e {@code ate} (inclusive), somados por tipo. */
    public List<SomaPorTipo> lancamentosPorTipo(Long tenantId, LocalDate de, LocalDate ate) {
        String sql = """
                select l.tipo, count(*) as quantidade, coalesce(sum(l.valor), 0) as valor
                from lancamento l
                where l.tenant_id = :tenantId and l.excluido_em is null and l.data between :de and :ate
                group by l.tipo
                """;
        MapSqlParameterSource params = tenant(tenantId)
                .addValue("de", Date.valueOf(de))
                .addValue("ate", Date.valueOf(ate));
        return jdbc.query(sql, params, (rs, i) -> new SomaPorTipo(
                TipoLancamento.valueOf(rs.getString("tipo")), rs.getLong("quantidade"), rs.getBigDecimal("valor")));
    }

    /** Últimos lançamentos (não excluídos) pela data de cadastro, com o nome do cliente. */
    public List<LancamentoResumo> ultimosLancamentos(Long tenantId, int limite) {
        String sql = """
                select l.id, l.cliente_id, c.nome as cliente_nome, l.tipo, l.data, l.descricao, l.valor,
                       l.categoria, l.origem, l.criado_em
                from lancamento l
                join cliente c on c.id = l.cliente_id and c.tenant_id = l.tenant_id
                where l.tenant_id = :tenantId and l.excluido_em is null
                order by l.criado_em desc, l.id desc
                limit :limite
                """;
        return jdbc.query(sql, tenant(tenantId).addValue("limite", limite), (rs, i) -> new LancamentoResumo(
                rs.getLong("id"),
                rs.getLong("cliente_id"),
                rs.getString("cliente_nome"),
                TipoLancamento.valueOf(rs.getString("tipo")),
                rs.getDate("data").toLocalDate(),
                rs.getString("descricao"),
                rs.getBigDecimal("valor"),
                CategoriaLancamento.valueOf(rs.getString("categoria")),
                rs.getString("origem"),
                rs.getTimestamp("criado_em").toInstant()));
    }

    private static MapSqlParameterSource tenant(Long tenantId) {
        return new MapSqlParameterSource("tenantId", tenantId);
    }

    private static ClienteResumo cliente(ResultSet rs, int linha) throws SQLException {
        return new ClienteResumo(
                rs.getLong("id"),
                rs.getString("nome"),
                rs.getString("nome_fantasia"),
                TipoPessoa.valueOf(rs.getString("tipo_pessoa")),
                rs.getString("documento"),
                rs.getBoolean("ativo"),
                rs.getBoolean("acesso_app_ativo"),
                rs.getTimestamp("data_inclusao").toInstant());
    }
}
