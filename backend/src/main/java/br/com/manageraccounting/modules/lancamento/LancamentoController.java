package br.com.manageraccounting.modules.lancamento;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import org.springframework.core.io.InputStreamResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import br.com.manageraccounting.core.security.AuthenticatedUser;
import br.com.manageraccounting.modules.lancamento.LancamentoService.ComprovanteDownload;
import br.com.manageraccounting.modules.lancamento.dto.CategoriaResponse;
import br.com.manageraccounting.modules.lancamento.dto.HistoricoResponse;
import br.com.manageraccounting.modules.lancamento.dto.LancamentoRequest;
import br.com.manageraccounting.modules.lancamento.dto.LancamentoResponse;
import br.com.manageraccounting.modules.lancamento.dto.ListaLancamentosResponse;
import jakarta.validation.Valid;

/**
 * Lançamentos dos clientes do escritório do contador logado. O tenant vem sempre do token, nunca da URL ou do corpo.
 * Alterar, excluir e mexer no comprovante pedem a {@code versao} que a tela carregou.
 */
@RestController
@RequestMapping("/api/lancamentos")
public class LancamentoController {

    private final LancamentoService service;

    public LancamentoController(LancamentoService service) {
        this.service = service;
    }

    /** Categorias fixas, na ordem de exibição. */
    @GetMapping("/categorias")
    public List<CategoriaResponse> categorias() {
        return service.categorias();
    }

    /**
     * Lista paginada (mais recentes primeiro) com os totais por tipo do filtro inteiro. Datas no formato
     * yyyy-MM-dd; {@code busca} procura na descrição.
     */
    @GetMapping
    public ListaLancamentosResponse listar(@AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) TipoLancamento tipo,
            @RequestParam(required = false) CategoriaLancamento categoria,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(required = false) String busca,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "" + LancamentoService.TAMANHO_PADRAO) int tamanho) {
        return service.listar(user.tenantId(), new LancamentoFiltro(clienteId, tipo, categoria, de, ate, busca),
                pagina, tamanho);
    }

    @GetMapping("/{id}")
    public LancamentoResponse obter(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        return service.obter(user.tenantId(), id);
    }

    @GetMapping("/{id}/historico")
    public List<HistoricoResponse> historico(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        return service.historico(user.tenantId(), id);
    }

    @PostMapping
    public ResponseEntity<LancamentoResponse> criar(@AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody LancamentoRequest request) {
        LancamentoResponse criado = service.criar(user.tenantId(), user.userId(), request);
        return ResponseEntity.created(URI.create("/api/lancamentos/" + criado.id())).body(criado);
    }

    @PutMapping("/{id}")
    public LancamentoResponse atualizar(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id,
            @Valid @RequestBody LancamentoRequest request) {
        return service.atualizar(user.tenantId(), user.userId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id,
            @RequestParam Long versao) {
        service.excluir(user.tenantId(), user.userId(), id, versao);
    }

    /** Envio do comprovante (multipart, campo {@code arquivo}). Troca o anterior, se houver. */
    @PostMapping(path = "/{id}/comprovante", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public LancamentoResponse anexarComprovante(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long id, @RequestParam Long versao, @RequestPart("arquivo") MultipartFile arquivo) {
        return service.anexarComprovante(user.tenantId(), user.userId(), id, versao, arquivo);
    }

    @DeleteMapping("/{id}/comprovante")
    public LancamentoResponse removerComprovante(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long id, @RequestParam Long versao) {
        return service.removerComprovante(user.tenantId(), user.userId(), id, versao);
    }

    /**
     * Arquivo do comprovante. Vai como anexo e com {@code nosniff}, para o navegador não tentar interpretar o
     * conteúdo como outra coisa; a tela baixa com o token e mostra a partir de um blob.
     */
    @GetMapping("/{id}/comprovante")
    public ResponseEntity<InputStreamResource> baixarComprovante(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long id) {
        ComprovanteDownload download = service.baixarComprovante(user.tenantId(), id);
        ContentDisposition disposicao = ContentDisposition.attachment()
                .filename(download.nome(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.contentType()))
                .contentLength(download.tamanho())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposicao.toString())
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(new InputStreamResource(download.conteudo()));
    }
}
