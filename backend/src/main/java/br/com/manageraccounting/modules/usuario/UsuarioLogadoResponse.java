package br.com.manageraccounting.modules.usuario;

/** Dados do contador logado, exibidos no cabeçalho do backoffice. */
public record UsuarioLogadoResponse(String login, String nomeCompleto, String email, PerfilAcesso perfil) {
}
