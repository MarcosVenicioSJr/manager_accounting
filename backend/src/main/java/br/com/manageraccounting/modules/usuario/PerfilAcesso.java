package br.com.manageraccounting.modules.usuario;

/**
 * Perfil de acesso do usuário dentro do escritório.
 * <ul>
 *   <li>ADMIN: tudo, inclusive gerenciar os usuários do escritório, desativar/reativar clientes e revogar o acesso
 *   ao app;</li>
 *   <li>USUARIO: consulta, cadastra e edita clientes e gera o token do app.</li>
 * </ul>
 */
public enum PerfilAcesso {
    ADMIN,
    USUARIO
}
