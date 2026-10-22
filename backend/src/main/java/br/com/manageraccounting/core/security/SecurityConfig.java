package br.com.manageraccounting.core.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import br.com.manageraccounting.modules.usuario.UsuarioRepository;

/**
 * Segurança da API: stateless, autenticação por JWT (header Authorization: Bearer).
 * <ul>
 *   <li>login, refresh e logout são públicos (o refresh token vai em cookie HttpOnly);</li>
 *   <li>a recuperação de senha ("Esqueceu sua senha?") é pública;</li>
 *   <li>a confirmação de troca de senha/e-mail pelo link do e-mail é pública (o token é a prova);</li>
 *   <li>primeiro-acesso exige estar logado com a senha temporária ({@code ROLE_PRIMEIRO_ACESSO});</li>
 *   <li>o aceite do convite de usuário é público (o token do e-mail é a prova);</li>
 *   <li>usuários do escritório, desativar/reativar cliente e revogar o acesso ao app exigem o administrador
 *   do escritório ({@code ROLE_ADMIN});</li>
 *   <li>todo o resto exige um contador com o primeiro acesso concluído ({@code ROLE_CONTADOR}).</li>
 * </ul>
 * CSRF fica desligado porque nenhum endpoint aceita autenticação por cookie além de refresh/logout, e esses
 * dependem do cookie SameSite (Strict por padrão).
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            JwtService jwtService,
            UsuarioRepository usuarioRepository,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/login", "/api/auth/refresh", "/api/auth/logout",
                                "/api/auth/recuperar-senha", "/api/auth/redefinir-senha",
                                "/api/auth/redefinir-senha/validar",
                                "/api/auth/confirmar-alteracao", "/api/auth/confirmar-alteracao/validar",
                                "/api/auth/convite/validar", "/api/auth/convite/aceitar").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/primeiro-acesso").hasRole("PRIMEIRO_ACESSO")
                        // Só o administrador do escritório
                        .requestMatchers("/api/usuarios", "/api/usuarios/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/clientes/*/desativar", "/api/clientes/*/reativar")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/clientes/*/acesso-app").hasRole("ADMIN")
                        .anyRequest().hasRole("CONTADOR"))
                .addFilterBefore(new JwtAuthenticationFilter(jwtService, usuarioRepository),
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
