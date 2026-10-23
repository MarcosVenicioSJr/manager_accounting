package br.com.manageraccounting.modules.equipe;

import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.manageraccounting.modules.equipe.dto.AceitarConviteRequest;
import br.com.manageraccounting.modules.equipe.dto.ConviteInfoResponse;
import br.com.manageraccounting.modules.equipe.dto.ValidarConviteRequest;
import jakarta.validation.Valid;

/**
 * Aceite do convite pelo link do e-mail (endpoints públicos: a pessoa ainda não tem senha).
 * <ol>
 *   <li>{@code POST /api/auth/convite/validar}: confere o link e devolve nome, login, e-mail e escritório;</li>
 *   <li>{@code POST /api/auth/convite/aceitar}: cria a senha e conclui o primeiro acesso.</li>
 * </ol>
 */
@RestController
@RequestMapping("/api/auth/convite")
public class ConviteController {

    private final EquipeService service;

    public ConviteController(EquipeService service) {
        this.service = service;
    }

    @PostMapping("/validar")
    public ConviteInfoResponse validar(@Valid @RequestBody ValidarConviteRequest request) {
        return service.validarConvite(request.token());
    }

    @PostMapping("/aceitar")
    public Map<String, String> aceitar(@Valid @RequestBody AceitarConviteRequest request) {
        String login = service.aceitarConvite(request.token(), request.nomeCompleto(), request.telefone(),
                request.novaSenha());
        return Map.of("login", login);
    }
}
