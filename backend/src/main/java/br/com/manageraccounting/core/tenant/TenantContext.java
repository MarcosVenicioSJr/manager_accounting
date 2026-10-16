package br.com.manageraccounting.core.tenant;

/**
 * Guarda o tenant da requisição atual.
 * É preenchido pelo filtro JWT (a partir do token, nunca de um dado enviado pelo cliente)
 * e limpo ao fim de cada requisição.
 */
public final class TenantContext {

    private static final ThreadLocal<Long> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setTenantId(Long tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static Long getTenantId() {
        return CURRENT_TENANT.get();
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
