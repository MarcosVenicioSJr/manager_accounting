package br.com.manageraccounting.modules.perfil;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.manageraccounting.modules.perfil.dto.ConfirmacaoInfoResponse;
import br.com.manageraccounting.modules.perfil.dto.ConfirmacaoTokenRequest;
import jakarta.validation.Valid;

/**
 * Confirmação pelo link do e-mail (endpoints públicos: o link pode ser aberto em outro aparelho, sem login).
 * O token prova o acesso ao e-mail; o pedido já tinha exigido a senha atual.
 * <ol>
 *   <li>{@code POST /api/auth/confirmar-alteracao/validar}: confere o link e diz o que ele confirma;</li>
 *   <li>{@code POST /api/auth/confirmar-alteracao}: aplica a troca (só quando a pessoa clica em "Confirmar",
 *   nunca ao abrir o link, para leitores de e-mail que abrem links sozinhos não confirmarem nada).</li>
 * </ol>
 */
@RestController
@RequestMapping("/api/auth/confirmar-alteracao")
public class ConfirmacaoAlteracaoController {

    private final PerfilService service;

    public ConfirmacaoAlteracaoController(PerfilService service) {
        this.service = service;
    }

    @PostMapping("/validar")
    public ConfirmacaoInfoResponse validar(@Valid @RequestBody ConfirmacaoTokenRequest request) {
        return service.validar(request.token());
    }

    @PostMapping
    public ConfirmacaoInfoResponse confirmar(@Valid @RequestBody ConfirmacaoTokenRequest request) {
        return new ConfirmacaoInfoResponse(service.confirmar(request.token()), null);
    }
}
