package br.com.manageraccounting.modules.usuario;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.manageraccounting.core.exception.UnauthorizedException;
import br.com.manageraccounting.core.security.AuthenticatedUser;

@RestController
@RequestMapping("/api/usuario")
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;

    public UsuarioController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /** Contador logado (o id vem do token). */
    @GetMapping("/me")
    @Transactional(readOnly = true)
    public UsuarioLogadoResponse me(@AuthenticationPrincipal AuthenticatedUser user) {
        Usuario usuario = usuarioRepository.findAtivoById(user.userId())
                .orElseThrow(() -> new UnauthorizedException("Sessão inválida"));
        return new UsuarioLogadoResponse(usuario.getLogin(), usuario.getNomeCompleto(), usuario.getEmail(),
                usuario.getPerfil());
    }
}
