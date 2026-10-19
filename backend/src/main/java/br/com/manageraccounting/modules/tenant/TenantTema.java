package br.com.manageraccounting.modules.tenant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Cores e logo do tenant. Campos nulos usam o padrão neutro do front.
 */
@Entity
@Table(name = "tenant_tema")
public class TenantTema {

    @Id
    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "cor_primaria", length = 7)
    private String corPrimaria;

    @Column(name = "cor_secundaria", length = 7)
    private String corSecundaria;

    /** Nome do arquivo dentro de web/public/tenants/&lt;guid&gt;/. */
    @Column(name = "logo_arquivo", length = 100)
    private String logoArquivo;

    protected TenantTema() {
    }

    public Long getTenantId() {
        return tenantId;
    }

    public String getCorPrimaria() {
        return corPrimaria;
    }

    public String getCorSecundaria() {
        return corSecundaria;
    }

    public String getLogoArquivo() {
        return logoArquivo;
    }
}
