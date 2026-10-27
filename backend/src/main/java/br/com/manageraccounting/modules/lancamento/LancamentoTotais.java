package br.com.manageraccounting.modules.lancamento;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

/**
 * Soma e quantidade por tipo, com o mesmo filtro da lista (a mesma {@link Specification}), para os totais do topo
 * da tela baterem com o que está listado.
 */
@Repository
public class LancamentoTotais {

    public record Total(long quantidade, BigDecimal valor) {
        static final Total ZERO = new Total(0, BigDecimal.ZERO.setScale(2));
    }

    private final EntityManager em;

    public LancamentoTotais(EntityManager em) {
        this.em = em;
    }

    public Map<TipoLancamento, Total> porTipo(Specification<Lancamento> filtro) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<Lancamento> root = query.from(Lancamento.class);
        query.multiselect(root.get("tipo").alias("tipo"), cb.count(root).alias("quantidade"),
                cb.sum(root.<BigDecimal>get("valor")).alias("valor"));
        query.where(filtro.toPredicate(root, query, cb));
        query.groupBy(root.get("tipo"));

        Map<TipoLancamento, Total> resultado = new EnumMap<>(TipoLancamento.class);
        for (Tuple t : em.createQuery(query).getResultList()) {
            BigDecimal valor = t.get("valor", BigDecimal.class);
            resultado.put(t.get("tipo", TipoLancamento.class), new Total(t.get("quantidade", Long.class),
                    valor == null ? BigDecimal.ZERO.setScale(2) : valor.setScale(2)));
        }
        return resultado;
    }
}
