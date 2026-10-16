package br.com.manageraccounting.core.security;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import br.com.manageraccounting.core.security.JwtService.TokenClaims;
import br.com.manageraccounting.core.tenant.TenantContext;
import br.com.manageraccounting.modules.usuario.Usuario;
import br.com.manageraccounting.modules.usuario.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Lê o access token do header Authorization, valida e preenche o SecurityContext e o TenantContext.
 * <p>
 * A cada requisição o usuário e o tenant são conferidos no banco: usuário ou tenant desativado perde o acesso
 * na hora, e quem ainda não concluiu o primeiro acesso recebe apenas a autoridade {@code ROLE_PRIMEIRO_ACESSO}
 * (só pode chamar o endpoint de primeiro acesso).
 * <p>
 * Não é um bean: é criado dentro do {@link SecurityConfig}, para não ser registrado duas vezes pelo servlet container.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    static final String ROLE_CONTADOR = "ROLE_CONTADOR";
    static final String ROLE_PRIMEIRO_ACESSO = "ROLE_PRIMEIRO_ACESSO";

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UsuarioRepository usuarioRepository) {
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            authenticate(request);
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private void authenticate(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return;
        }

        Optional<TokenClaims> claims = jwtService.parse(header.substring(BEARER_PREFIX.length()).trim());
        if (claims.isEmpty()) {
            return;
        }

        Optional<Usuario> usuario = usuarioRepository.findAtivoById(claims.get().userId());
        if (usuario.isEmpty() || !usuario.get().getTenantId().equals(claims.get().tenantId())) {
            return;
        }

        Usuario user = usuario.get();
        String authority = user.isPrimeiroAcessoConcluido() ? ROLE_CONTADOR : ROLE_PRIMEIRO_ACESSO;

        UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
                new AuthenticatedUser(user.getId(), user.getTenantId()),
                null,
                List.of(new SimpleGrantedAuthority(authority)));
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        TenantContext.setTenantId(user.getTenantId());
    }
}
