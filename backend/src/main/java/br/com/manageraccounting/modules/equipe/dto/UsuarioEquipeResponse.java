package br.com.manageraccounting.modules.equipe.dto;

import java.time.Instant;

import br.com.manageraccounting.modules.usuario.PerfilAcesso;

/**
 * Usuário do escritório na lista. {@code situacao}: ATIVO, PENDENTE (ainda não criou a senha / não concluiu o
 * primeiro acesso) ou INATIVO. {@code conviteExpiraEm}: validade do convite em aberto, se houver.
 * {@code voce}: é o administrador logado.
 */
public record UsuarioEquipeResponse(
        Long id,
        String login,
        String nomeCompleto,
        String email,
        PerfilAcesso perfil,
        Situacao situacao,
        Instant conviteExpiraEm,
        Instant dataInclusao,
        boolean voce) {

    public enum Situacao {
        ATIVO,
        PENDENTE,
        INATIVO
    }
}
