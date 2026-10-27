package br.com.manageraccounting.modules.relatorio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.manageraccounting.core.exception.BusinessException;
import br.com.manageraccounting.core.exception.NotFoundException;
import br.com.manageraccounting.modules.cliente.Cliente;
import br.com.manageraccounting.modules.cliente.ClienteRepository;
import br.com.manageraccounting.modules.lancamento.CategoriaLancamento;
import br.com.manageraccounting.modules.lancamento.TipoLancamento;
import br.com.manageraccounting.modules.relatorio.RelatorioConsultas.LinhaCategoria;
import br.com.manageraccounting.modules.relatorio.RelatorioConsultas.LinhaExtrato;
import br.com.manageraccounting.modules.relatorio.RelatorioConsultas.LinhaResumo;
import br.com.manageraccounting.modules.relatorio.dto.CategoriasResponse;
import br.com.manageraccounting.modules.relatorio.dto.ExtratoClienteResponse;
import br.com.manageraccounting.modules.relatorio.dto.ExtratoClienteResponse.ClienteRelatorio;
import br.com.manageraccounting.modules.relatorio.dto.ExtratoClienteResponse.Item;
import br.com.manageraccounting.modules.relatorio.dto.ExtratoClienteResponse.TotalCategoria;
import br.com.manageraccounting.modules.relatorio.dto.ExtratoClienteResponse.TotalTipo;
import br.com.manageraccounting.modules.relatorio.dto.ResumoEscritorioResponse;
import br.com.manageraccounting.modules.relatorio.dto.ResumoEscritorioResponse.Linha;
import br.com.manageraccounting.modules.tenant.Tenant;
import br.com.manageraccounting.modules.tenant.TenantRepository;

/**
 * Monta os dados dos relatórios. Período obrigatório, de no máximo um ano (366 dias). O extrato aceita cliente
 * inativo, para dar para tirar o histórico de quem saiu do escritório.
 */
@Service
public class RelatorioService {

    static final long DIAS_MAXIMOS = 366;
    static final int LIMITE_EXTRATO = 5000;
    static final String MSG_CLIENTE_NAO_ENCONTRADO = "Cliente não encontrado.";

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private final RelatorioConsultas consultas;
    private final ClienteRepository clienteRepository;
    private final TenantRepository tenantRepository;
    private final Clock clock;

    public RelatorioService(RelatorioConsultas consultas, ClienteRepository clienteRepository,
            TenantRepository tenantRepository, Clock clock) {
        this.consultas = consultas;
        this.clienteRepository = clienteRepository;
        this.tenantRepository = tenantRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ExtratoClienteResponse extrato(Long tenantId, Long clienteId, LocalDate de, LocalDate ate) {
        if (clienteId == null) {
            throw new BusinessException("Escolha o cliente.");
        }
        validarPeriodo(de, ate);
        Cliente cliente = clienteRepository.findByIdAndTenantId(clienteId, tenantId)
                .orElseThrow(() -> new NotFoundException(MSG_CLIENTE_NAO_ENCONTRADO));

        // Busca uma linha a mais para saber se passou do limite
        List<LinhaExtrato> linhas = consultas.extrato(tenantId, clienteId, de, ate, LIMITE_EXTRATO + 1);
        if (linhas.size() > LIMITE_EXTRATO) {
            throw new BusinessException("O período tem mais de 5.000 lançamentos. Escolha um período menor.");
        }

        List<Item> itens = linhas.stream()
                .map(l -> new Item(l.id(), l.data(), l.tipo(), l.descricao(), l.categoria(),
                        l.categoria().getRotulo(), l.valor().setScale(2), l.origem(), l.temComprovante()))
                .toList();

        return new ExtratoClienteResponse(nomeEscritorio(tenantId),
                new ClienteRelatorio(cliente.getId(), cliente.getNome(), cliente.getNomeFantasia(),
                        cliente.getTipoPessoa(), cliente.getDocumento(), cliente.isAtivo()),
                de, ate, clock.instant(), itens, totaisPorTipo(itens), totaisPorCategoria(itens), itens.size(),
                soma(itens.stream().map(Item::valor).toList()));
    }

    @Transactional(readOnly = true)
    public ResumoEscritorioResponse resumo(Long tenantId, LocalDate de, LocalDate ate) {
        validarPeriodo(de, ate);
        List<Linha> linhas = consultas.resumo(tenantId, de, ate).stream()
                .map(RelatorioService::linha)
                .sorted(Comparator.comparing(Linha::total).reversed()
                        .thenComparing(l -> l.nome().toLowerCase(Locale.ROOT))
                        .thenComparing(Linha::clienteId))
                .toList();

        long comMovimento = linhas.stream().filter(l -> l.quantidade() > 0).count();
        long semMovimento = linhas.stream().filter(l -> l.quantidade() == 0).count();
        Linha totais = new Linha(null, null, null, null, null,
                linhas.stream().mapToLong(Linha::quantidade).sum(),
                soma(linhas.stream().map(Linha::compras).toList()),
                soma(linhas.stream().map(Linha::dividas).toList()),
                soma(linhas.stream().map(Linha::despesas).toList()),
                soma(linhas.stream().map(Linha::total).toList()));

        return new ResumoEscritorioResponse(nomeEscritorio(tenantId), de, ate, clock.instant(), linhas, totais,
                comMovimento, semMovimento);
    }

    /**
     * Gastos por categoria de um cliente ({@code clienteId}) ou de todos os clientes ({@code null}). O cliente pode
     * estar inativo; de outro escritório responde "não encontrado".
     */
    @Transactional(readOnly = true)
    public CategoriasResponse porCategoria(Long tenantId, Long clienteId, LocalDate de, LocalDate ate) {
        validarPeriodo(de, ate);
        ClienteRelatorio cliente = null;
        if (clienteId != null) {
            Cliente c = clienteRepository.findByIdAndTenantId(clienteId, tenantId)
                    .orElseThrow(() -> new NotFoundException(MSG_CLIENTE_NAO_ENCONTRADO));
            cliente = new ClienteRelatorio(c.getId(), c.getNome(), c.getNomeFantasia(), c.getTipoPessoa(),
                    c.getDocumento(), c.isAtivo());
        }

        List<LinhaCategoria> brutas = consultas.porCategoria(tenantId, clienteId, de, ate);
        BigDecimal totalGeral = soma(brutas.stream()
                .map(l -> l.compras().add(l.dividas()).add(l.despesas()))
                .toList());
        List<CategoriasResponse.Linha> linhas = brutas.stream()
                .map(l -> linhaCategoria(l, totalGeral))
                .sorted(Comparator.comparing(CategoriasResponse.Linha::total).reversed()
                        .thenComparing(l -> l.categoria().ordinal()))
                .toList();

        long clientes = brutas.isEmpty() ? 0 : consultas.clientesComLancamento(tenantId, clienteId, de, ate);
        CategoriasResponse.Linha totais = new CategoriasResponse.Linha(null, null,
                linhas.stream().mapToLong(CategoriasResponse.Linha::quantidade).sum(), clientes,
                soma(linhas.stream().map(CategoriasResponse.Linha::compras).toList()),
                soma(linhas.stream().map(CategoriasResponse.Linha::dividas).toList()),
                soma(linhas.stream().map(CategoriasResponse.Linha::despesas).toList()),
                totalGeral,
                totalGeral.signum() > 0 ? new BigDecimal("100.0") : new BigDecimal("0.0"));

        return new CategoriasResponse(nomeEscritorio(tenantId), cliente, de, ate, clock.instant(), linhas, totais,
                clientes);
    }

    // ---------- apoio ----------

    private static CategoriasResponse.Linha linhaCategoria(LinhaCategoria l, BigDecimal totalGeral) {
        BigDecimal compras = l.compras().setScale(2);
        BigDecimal dividas = l.dividas().setScale(2);
        BigDecimal despesas = l.despesas().setScale(2);
        BigDecimal total = compras.add(dividas).add(despesas);
        BigDecimal percentual = totalGeral.signum() > 0
                ? total.multiply(BigDecimal.valueOf(100)).divide(totalGeral, 1, RoundingMode.HALF_UP)
                : new BigDecimal("0.0");
        return new CategoriasResponse.Linha(l.categoria(), l.categoria().getRotulo(), l.quantidade(), l.clientes(),
                compras, dividas, despesas, total, percentual);
    }

    static void validarPeriodo(LocalDate de, LocalDate ate) {
        if (de == null || ate == null) {
            throw new BusinessException("Informe o período (data inicial e final).");
        }
        if (de.isAfter(ate)) {
            throw new BusinessException("A data inicial é depois da data final.");
        }
        if (ChronoUnit.DAYS.between(de, ate) + 1 > DIAS_MAXIMOS) {
            throw new BusinessException("O período pode ter no máximo um ano.");
        }
    }

    private String nomeEscritorio(Long tenantId) {
        return tenantRepository.findById(tenantId).map(Tenant::getNomeEmpresa).orElse("");
    }

    private static Linha linha(LinhaResumo r) {
        BigDecimal compras = r.compras().setScale(2);
        BigDecimal dividas = r.dividas().setScale(2);
        BigDecimal despesas = r.despesas().setScale(2);
        return new Linha(r.clienteId(), r.nome(), r.tipoPessoa(), r.documento(), r.ativo(), r.quantidade(),
                compras, dividas, despesas, compras.add(dividas).add(despesas));
    }

    static List<TotalTipo> totaisPorTipo(List<Item> itens) {
        Map<TipoLancamento, long[]> qtd = new EnumMap<>(TipoLancamento.class);
        Map<TipoLancamento, BigDecimal> valor = new EnumMap<>(TipoLancamento.class);
        for (Item i : itens) {
            qtd.computeIfAbsent(i.tipo(), t -> new long[1])[0]++;
            valor.merge(i.tipo(), i.valor(), BigDecimal::add);
        }
        return Arrays.stream(TipoLancamento.values())
                .map(t -> new TotalTipo(t, qtd.containsKey(t) ? qtd.get(t)[0] : 0, valor.getOrDefault(t, ZERO)))
                .toList();
    }

    static List<TotalCategoria> totaisPorCategoria(List<Item> itens) {
        Map<CategoriaLancamento, long[]> qtd = new EnumMap<>(CategoriaLancamento.class);
        Map<CategoriaLancamento, BigDecimal> valor = new EnumMap<>(CategoriaLancamento.class);
        for (Item i : itens) {
            qtd.computeIfAbsent(i.categoria(), c -> new long[1])[0]++;
            valor.merge(i.categoria(), i.valor(), BigDecimal::add);
        }
        List<TotalCategoria> lista = new ArrayList<>();
        valor.forEach((c, v) -> lista.add(new TotalCategoria(c, c.getRotulo(), qtd.get(c)[0], v)));
        lista.sort(Comparator.comparing(TotalCategoria::valor).reversed()
                .thenComparing(t -> t.categoria().ordinal()));
        return lista;
    }

    private static BigDecimal soma(List<BigDecimal> valores) {
        return valores.stream().reduce(ZERO, BigDecimal::add);
    }
}
