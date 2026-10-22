package br.com.manageraccounting.modules.equipe;

import java.net.URI;
import java.util.List;

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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.manageraccounting.core.security.AuthenticatedUser;
import br.com.manageraccounting.modules.equipe.dto.AlterarPerfilRequest;
import br.com.manageraccounting.modules.equipe.dto.ConvidarUsuarioRequest;
import br.com.manageraccounting.modules.equipe.dto.UsuarioEquipeResponse;
import jakarta.validation.Valid;

/** Usuários do escritório do administrador logado (a rota exige ROLE_ADMIN, ver SecurityConfig). */
@RestController
@RequestMapping("/api/usuarios")
public class EquipeController {

    private final EquipeService service;

    public EquipeController(EquipeService service) {
        this.service = service;
    }

    @GetMapping
    public List<UsuarioEquipeResponse> listar(@AuthenticationPrincipal AuthenticatedUser user) {
        return service.listar(user.tenantId(), user.userId());
    }

    /** Convida: cria o usuário pendente e envia o link para ele criar a senha. */
    @PostMapping
    public ResponseEntity<UsuarioEquipeResponse> convidar(@AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody ConvidarUsuarioRequest request) {
        UsuarioEquipeResponse criado = service.convidar(user.tenantId(), user.userId(), request);
        return ResponseEntity.created(URI.create("/api/usuarios/" + criado.id())).body(criado);
    }

    @PostMapping("/{id}/convite")
    public UsuarioEquipeResponse reenviarConvite(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long id) {
        return service.reenviarConvite(user.tenantId(), user.userId(), id);
    }

    /** Cancela o convite de quem ainda não criou a senha (o usuário é apagado e o login fica livre). */
    @DeleteMapping("/{id}/convite")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelarConvite(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        service.cancelarConvite(user.tenantId(), user.userId(), id);
    }

    @PutMapping("/{id}/perfil")
    public UsuarioEquipeResponse alterarPerfil(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id,
            @Valid @RequestBody AlterarPerfilRequest request) {
        return service.alterarPerfil(user.tenantId(), user.userId(), id, request.perfil());
    }

    @PostMapping("/{id}/desativar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desativar(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        service.desativar(user.tenantId(), user.userId(), id);
    }

    @PostMapping("/{id}/reativar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reativar(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        service.reativar(user.tenantId(), user.userId(), id);
    }
}
