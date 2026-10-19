package br.com.manageraccounting.modules.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.manageraccounting.modules.auth.dto.RecuperarSenhaRequest;
import br.com.manageraccounting.modules.auth.dto.RecuperarSenhaResponse;
import br.com.manageraccounting.modules.auth.dto.RedefinirSenhaRequest;
import br.com.manageraccounting.modules.auth.dto.ValidarTokenRedefinicaoRequest;
import jakarta.validation.Valid;

/**
 * "Esqueceu sua senha?" (endpoints públicos).
 * <ol>
 *   <li>{@code POST /api/auth/recuperar-senha}: envia o link por e-mail (resposta sempre igual);</li>
 *   <li>{@code POST /api/auth/redefinir-senha/validar}: confere o link antes de mostrar o formulário;</li>
 *   <li>{@code POST /api/auth/redefinir-senha}: grava a nova senha e consome o link.</li>
 * </ol>
 */
@RestController
@RequestMapping("/api/auth")
public class RecuperacaoSenhaController {

    private final RecuperacaoSenhaService service;

    public RecuperacaoSenhaController(RecuperacaoSenhaService service) {
        this.service = service;
    }

    @PostMapping("/recuperar-senha")
    public ResponseEntity<RecuperarSenhaResponse> recuperar(@Valid @RequestBody RecuperarSenhaRequest request) {
        service.solicitar(request.login());
        String mensagem = "Se o usuário estiver cadastrado e já tiver concluído o primeiro acesso, enviaremos um "
                + "link para o e-mail dele. O link vale por " + service.getTokenTtl().toMinutes() + " minutos.";
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new RecuperarSenhaResponse(mensagem));
    }

    @PostMapping("/redefinir-senha/validar")
    public ResponseEntity<Void> validar(@Valid @RequestBody ValidarTokenRedefinicaoRequest request) {
        service.validar(request.token());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/redefinir-senha")
    public ResponseEntity<Void> redefinir(@Valid @RequestBody RedefinirSenhaRequest request) {
        service.redefinir(request.token(), request.novaSenha());
        return ResponseEntity.noContent().build();
    }
}
