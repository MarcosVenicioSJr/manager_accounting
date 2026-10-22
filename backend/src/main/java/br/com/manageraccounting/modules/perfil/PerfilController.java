package br.com.manageraccounting.modules.perfil;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.manageraccounting.core.security.AuthenticatedUser;
import br.com.manageraccounting.modules.perfil.dto.AtualizarPerfilRequest;
import br.com.manageraccounting.modules.perfil.dto.PerfilResponse;
import br.com.manageraccounting.modules.perfil.dto.PerfilResponse.TrocaPendente;
import br.com.manageraccounting.modules.perfil.dto.TrocarEmailRequest;
import br.com.manageraccounting.modules.perfil.dto.TrocarSenhaRequest;
import jakarta.validation.Valid;

/** Meu perfil do contador logado (o usuário vem sempre do token). */
@RestController
@RequestMapping("/api/perfil")
public class PerfilController {

    private final PerfilService service;

    public PerfilController(PerfilService service) {
        this.service = service;
    }

    @GetMapping
    public PerfilResponse obter(@AuthenticationPrincipal AuthenticatedUser user) {
        return service.obter(user.userId());
    }

    /** Nome e telefone: mudam na hora. */
    @PutMapping
    public PerfilResponse atualizar(@AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody AtualizarPerfilRequest request) {
        return service.atualizarDados(user.userId(), request);
    }

    /** Pede a troca de senha. 202: o link de confirmação foi para o e-mail atual; a senha ainda não mudou. */
    @PostMapping("/senha")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public TrocaPendente trocarSenha(@AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody TrocarSenhaRequest request) {
        return service.solicitarTrocaSenha(user.userId(), request.senhaAtual(), request.novaSenha());
    }

    /** Pede a troca de e-mail. 202: o link de confirmação foi para o novo e-mail; o e-mail ainda não mudou. */
    @PostMapping("/email")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public TrocaPendente trocarEmail(@AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody TrocarEmailRequest request) {
        return service.solicitarTrocaEmail(user.userId(), request.senhaAtual(), request.novoEmail());
    }

    /** Cancela o pedido em aberto ({@code SENHA} ou {@code EMAIL}): o link do e-mail deixa de funcionar. */
    @DeleteMapping("/alteracoes/{tipo}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelar(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable TipoAlteracao tipo) {
        service.cancelar(user.userId(), tipo);
    }
}
