package br.com.manageraccounting.modules.auth;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.manageraccounting.core.security.AuthenticatedUser;
import br.com.manageraccounting.modules.auth.dto.AuthResponse;
import br.com.manageraccounting.modules.auth.dto.LoginRequest;
import br.com.manageraccounting.modules.auth.dto.PrimeiroAcessoRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final RefreshCookieFactory cookieFactory;

    public AuthController(AuthService authService, RefreshTokenService refreshTokenService,
            RefreshCookieFactory cookieFactory) {
        this.authService = authService;
        this.refreshTokenService = refreshTokenService;
        this.cookieFactory = cookieFactory;
    }

    /** Login: devolve o access token no corpo e o refresh token em cookie HttpOnly. */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResult result = authService.login(request.login(), request.senha());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        cookieFactory.criar(result.refreshToken(), refreshTokenService.getTtl()).toString())
                .body(result.response());
    }

    /** Gera um novo access token a partir do cookie de refresh. */
    @PostMapping("/refresh")
    public AuthResponse refresh(
            @CookieValue(name = RefreshCookieFactory.COOKIE_NAME, required = false) String refreshToken) {
        return authService.refresh(refreshToken);
    }

    /** Revoga a sessão de refresh e apaga o cookie. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = RefreshCookieFactory.COOKIE_NAME, required = false) String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieFactory.expirar().toString())
                .build();
    }

    /** Primeiro acesso: troca a senha temporária e completa os dados. Só para quem ainda não concluiu. */
    @PostMapping("/primeiro-acesso")
    public ResponseEntity<Void> primeiroAcesso(@AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody PrimeiroAcessoRequest request) {
        authService.concluirPrimeiroAcesso(user.userId(), request);
        return ResponseEntity.noContent().build();
    }
}
