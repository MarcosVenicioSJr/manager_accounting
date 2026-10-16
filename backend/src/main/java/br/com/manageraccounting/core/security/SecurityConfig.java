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
 *   <li>primeiro-acesso exige estar logado com a senha temporária ({@code ROLE_PRIMEIRO_ACESSO});</li>
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
                                "/api/auth/login", "/api/auth/refresh", "/api/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/primeiro-acesso").hasRole("PRIMEIRO_ACESSO")
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
