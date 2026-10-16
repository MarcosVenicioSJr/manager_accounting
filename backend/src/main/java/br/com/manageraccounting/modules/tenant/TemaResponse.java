package br.com.manageraccounting.modules.tenant;

/**
 * Tema do tenant devolvido após o login. {@code logoUrl} é relativo à raiz do web
 * (ex.: /tenants/&lt;guid&gt;/logo.png); campos nulos significam "usar o padrão".
 */
public record TemaResponse(String nomeEmpresa, String logoUrl, String corPrimaria, String corSecundaria) {
}
