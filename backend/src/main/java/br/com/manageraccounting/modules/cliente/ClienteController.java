package br.com.manageraccounting.modules.cliente;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.manageraccounting.core.security.AuthenticatedUser;
import br.com.manageraccounting.modules.cliente.dto.ClienteListaItem;
import br.com.manageraccounting.modules.cliente.dto.ClienteRequest;
import br.com.manageraccounting.modules.cliente.dto.ClienteResponse;
import br.com.manageraccounting.modules.cliente.dto.ClientesResumoResponse;
import br.com.manageraccounting.modules.cliente.dto.PaginaResponse;
import br.com.manageraccounting.modules.cliente.dto.TokenGeradoResponse;
import jakarta.validation.Valid;

/**
 * Clientes do escritório do contador logado. O tenant vem sempre do token, nunca da URL ou do corpo.
 */
@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService service;

    public ClienteController(ClienteService service) {
        this.service = service;
    }

    /** Lista paginada, ordenada por nome. {@code busca} procura em nome, nome fantasia, e-mail e CPF/CNPJ. */
    @GetMapping
    public PaginaResponse<ClienteListaItem> listar(@AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) String busca,
            @RequestParam(defaultValue = "ATIVOS") StatusFiltro status,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "" + ClienteService.TAMANHO_PADRAO) int tamanho) {
        return service.listar(user.tenantId(), busca, status, pagina, tamanho);
    }

    @GetMapping("/resumo")
    public ClientesResumoResponse resumo(@AuthenticationPrincipal AuthenticatedUser user) {
        return service.resumo(user.tenantId());
    }

    @GetMapping("/{id}")
    public ClienteResponse obter(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        return service.obter(user.tenantId(), id);
    }

    @PostMapping
    public ResponseEntity<ClienteResponse> criar(@AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody ClienteRequest request) {
        ClienteResponse criado = service.criar(user.tenantId(), request);
        return ResponseEntity.created(URI.create("/api/clientes/" + criado.id())).body(criado);
    }

    @PutMapping("/{id}")
    public ClienteResponse atualizar(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id,
            @Valid @RequestBody ClienteRequest request) {
        return service.atualizar(user.tenantId(), id, request);
    }

    @PostMapping("/{id}/desativar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desativar(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        service.desativar(user.tenantId(), user.userId(), id);
    }

    @PostMapping("/{id}/reativar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reativar(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        service.reativar(user.tenantId(), id);
    }

    /** Gera (ou gera de novo) o token do app. O valor vem só nesta resposta. */
    @PostMapping("/{id}/acesso-app")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenGeradoResponse gerarToken(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        return service.gerarToken(user.tenantId(), user.userId(), id);
    }

    @DeleteMapping("/{id}/acesso-app")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revogarToken(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        service.revogarToken(user.tenantId(), user.userId(), id);
    }
}
