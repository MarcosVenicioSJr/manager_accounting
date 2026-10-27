package br.com.manageraccounting.modules.relatorio;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import br.com.manageraccounting.modules.cliente.TipoPessoa;
import br.com.manageraccounting.modules.lancamento.CategoriaLancamento;
import br.com.manageraccounting.modules.lancamento.OrigemLancamento;
import br.com.manageraccounting.modules.lancamento.TipoLancamento;

/**
 * Consultas dos relatórios em SQL direto. Toda consulta filtra pelo {@code tenant_id} recebido (vem do token) e
 * ignora lançamentos excluídos.
 */
@Repository
public class RelatorioConsultas {

    private final NamedParameterJdbcTemplate jdbc;

    public RelatorioConsultas(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record LinhaExtrato(Long id, LocalDate data, TipoLancamento tipo, String descricao,
            CategoriaLancamento categoria, BigDecimal valor, OrigemLancamento origem, boolean temComprovante) {
    }

    public record LinhaResumo(Long clienteId, String nome, TipoPessoa tipoPessoa, String documento, boolean ativo,
            long quantidade, BigDecimal compras, BigDecimal dividas, BigDecimal despesas) {
    }

    public record LinhaCategoria(CategoriaLancamento categoria, long quantidade, long clientes, BigDecimal compras,
            BigDecimal dividas, BigDecimal despesas) {
    }

    /**
     * Totais por categoria no período, de um cliente ({@code clienteId}) ou de todos ({@code null}).
     * Só voltam as categorias com lançamento.
     */
    public List<LinhaCategoria> porCategoria(Long tenantId, Long clienteId, LocalDate de, LocalDate ate) {
        String sql = """
                select l.categoria, count(*) as quantidade, count(distinct l.cliente_id) as clientes,
                       coalesce(sum(l.valor) filter (where l.tipo = 'COMPRA'), 0) as compras,
                       coalesce(sum(l.valor) filter (where l.tipo = 'DIVIDA'), 0) as dividas,
                       coalesce(sum(l.valor) filter (where l.tipo = 'DESPESA'), 0) as despesas
                from lancamento l
                where l.tenant_id = :tenantId and l.excluido_em is null and l.data between :de and :ate
                %s
                group by l.categoria
                """.formatted(clienteId == null ? "" : "and l.cliente_id = :clienteId");
        MapSqlParameterSource params = periodo(tenantId, de, ate);
        if (clienteId != null) {
            params.addValue("clienteId", clienteId);
        }
        return jdbc.query(sql, params, (rs, i) -> new LinhaCategoria(
                CategoriaLancamento.valueOf(rs.getString("categoria")),
                rs.getLong("quantidade"),
                rs.getLong("clientes"),
                rs.getBigDecimal("compras"),
                rs.getBigDecimal("dividas"),
                rs.getBigDecimal("despesas")));
    }

    /** Quantos clientes diferentes lançaram no período (de todos, ou só o cliente informado). */
    public long clientesComLancamento(Long tenantId, Long clienteId, LocalDate de, LocalDate ate) {
        String sql = """
                select count(distinct l.cliente_id)
                from lancamento l
                where l.tenant_id = :tenantId and l.excluido_em is null and l.data between :de and :ate
                %s
                """.formatted(clienteId == null ? "" : "and l.cliente_id = :clienteId");
        MapSqlParameterSource params = periodo(tenantId, de, ate);
        if (clienteId != null) {
            params.addValue("clienteId", clienteId);
        }
        Long n = jdbc.queryForObject(sql, params, (rs, i) -> rs.getLong(1));
        return n == null ? 0 : n;
    }

    /** Lançamentos do cliente no período, do mais antigo para o mais recente. Traz até {@code limite} linhas. */
    public List<LinhaExtrato> extrato(Long tenantId, Long clienteId, LocalDate de, LocalDate ate, int limite) {
        String sql = """
                select l.id, l.data, l.tipo, l.descricao, l.categoria, l.valor, l.origem,
                       l.comprovante_chave is not null as tem_comprovante
                from lancamento l
                where l.tenant_id = :tenantId and l.cliente_id = :clienteId and l.excluido_em is null
                  and l.data between :de and :ate
                order by l.data, l.id
                limit :limite
                """;
        MapSqlParameterSource params = periodo(tenantId, de, ate)
                .addValue("clienteId", clienteId)
                .addValue("limite", limite);
        return jdbc.query(sql, params, (rs, i) -> new LinhaExtrato(
                rs.getLong("id"),
                rs.getDate("data").toLocalDate(),
                TipoLancamento.valueOf(rs.getString("tipo")),
                rs.getString("descricao"),
                CategoriaLancamento.valueOf(rs.getString("categoria")),
                rs.getBigDecimal("valor"),
                OrigemLancamento.valueOf(rs.getString("origem")),
                rs.getBoolean("tem_comprovante")));
    }

    /**
     * Totais por cliente no período: clientes ativos (com ou sem lançamento) e inativos que tiveram lançamento.
     */
    public List<LinhaResumo> resumo(Long tenantId, LocalDate de, LocalDate ate) {
        String sql = """
                select c.id, c.nome, c.tipo_pessoa, c.documento, c.ativo,
                       count(l.id) as quantidade,
                       coalesce(sum(l.valor) filter (where l.tipo = 'COMPRA'), 0) as compras,
                       coalesce(sum(l.valor) filter (where l.tipo = 'DIVIDA'), 0) as dividas,
                       coalesce(sum(l.valor) filter (where l.tipo = 'DESPESA'), 0) as despesas
                from cliente c
                left join lancamento l
                       on l.cliente_id = c.id and l.tenant_id = c.tenant_id and l.excluido_em is null
                      and l.data between :de and :ate
                where c.tenant_id = :tenantId
                group by c.id, c.nome, c.tipo_pessoa, c.documento, c.ativo
                having c.ativo or count(l.id) > 0
                """;
        return jdbc.query(sql, periodo(tenantId, de, ate), (rs, i) -> new LinhaResumo(
                rs.getLong("id"),
                rs.getString("nome"),
                TipoPessoa.valueOf(rs.getString("tipo_pessoa")),
                rs.getString("documento"),
                rs.getBoolean("ativo"),
                rs.getLong("quantidade"),
                rs.getBigDecimal("compras"),
                rs.getBigDecimal("dividas"),
                rs.getBigDecimal("despesas")));
    }

    private static MapSqlParameterSource periodo(Long tenantId, LocalDate de, LocalDate ate) {
        return new MapSqlParameterSource("tenantId", tenantId)
                .addValue("de", Date.valueOf(de))
                .addValue("ate", Date.valueOf(ate));
    }
}
