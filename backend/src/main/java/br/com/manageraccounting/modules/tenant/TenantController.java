package br.com.manageraccounting.modules.tenant;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.manageraccounting.core.security.AuthenticatedUser;

@RestController
@RequestMapping("/api/tenant")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    /** Tema (nome, logo e cores) do tenant do usuário logado. O tenant vem do token, nunca do cliente. */
    @GetMapping("/tema")
    public TemaResponse tema(@AuthenticationPrincipal AuthenticatedUser user) {
        return tenantService.obterTema(user.tenantId());
    }
}
