package br.com.manageraccounting.modules.dashboard;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.manageraccounting.core.security.AuthenticatedUser;
import br.com.manageraccounting.modules.dashboard.dto.DashboardResponse;

/**
 * Página inicial do backoffice. O tenant vem do token; o bloco da equipe só vai para administradores.
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping
    public DashboardResponse obter(@AuthenticationPrincipal AuthenticatedUser user, Authentication authentication) {
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(a -> ROLE_ADMIN.equals(a.getAuthority()));
        return service.montar(user.tenantId(), admin);
    }
}
