package br.com.manageraccounting.modules.relatorio;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.manageraccounting.core.security.AuthenticatedUser;
import br.com.manageraccounting.modules.relatorio.dto.CategoriasResponse;
import br.com.manageraccounting.modules.relatorio.dto.ExtratoClienteResponse;
import br.com.manageraccounting.modules.relatorio.dto.ResumoEscritorioResponse;

/**
 * Dados dos relatórios do escritório do contador logado (o tenant vem do token). Datas no formato yyyy-MM-dd.
 * O front transforma a resposta em PDF ou Excel.
 */
@RestController
@RequestMapping("/api/relatorios")
public class RelatorioController {

    private final RelatorioService service;

    public RelatorioController(RelatorioService service) {
        this.service = service;
    }

    @GetMapping("/extrato")
    public ExtratoClienteResponse extrato(@AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        return service.extrato(user.tenantId(), clienteId, de, ate);
    }

    @GetMapping("/resumo")
    public ResumoEscritorioResponse resumo(@AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        return service.resumo(user.tenantId(), de, ate);
    }

    /** Gastos por categoria; sem {@code clienteId}, de todos os clientes do escritório. */
    @GetMapping("/categorias")
    public CategoriasResponse porCategoria(@AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        return service.porCategoria(user.tenantId(), clienteId, de, ate);
    }
}
