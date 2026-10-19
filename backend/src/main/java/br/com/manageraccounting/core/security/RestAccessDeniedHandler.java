package br.com.manageraccounting.core.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Resposta 403 em JSON (ProblemDetail).
 * Quando o usuário ainda não concluiu o primeiro acesso, devolve {@code codigo = PRIMEIRO_ACESSO_PENDENTE}
 * para o front redirecionar para a tela de primeiro acesso.
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    public static final String CODIGO_PRIMEIRO_ACESSO_PENDENTE = "PRIMEIRO_ACESSO_PENDENTE";

    private final ObjectMapper objectMapper;

    public RestAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        ProblemDetail problem;
        if (isPrimeiroAcessoPendente()) {
            problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN,
                    "Conclua o primeiro acesso para continuar");
            problem.setProperty("codigo", CODIGO_PRIMEIRO_ACESSO_PENDENTE);
        } else {
            problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Acesso negado");
        }

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), problem);
    }

    private boolean isPrimeiroAcessoPendente() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> JwtAuthenticationFilter.ROLE_PRIMEIRO_ACESSO.equals(authority.getAuthority()));
    }
}
