package br.com.manageraccounting.modules.tenant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.manageraccounting.core.exception.UnauthorizedException;

@Service
public class TenantService {

    private final TenantRepository tenantRepository;
    private final TenantTemaRepository temaRepository;

    public TenantService(TenantRepository tenantRepository, TenantTemaRepository temaRepository) {
        this.tenantRepository = tenantRepository;
        this.temaRepository = temaRepository;
    }

    @Transactional(readOnly = true)
    public TemaResponse obterTema(Long tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .filter(Tenant::isAtivo)
                .orElseThrow(() -> new UnauthorizedException("Sessão inválida"));

        return temaRepository.findById(tenantId)
                .map(tema -> new TemaResponse(
                        tenant.getNomeEmpresa(),
                        montarLogoUrl(tenant, tema),
                        tema.getCorPrimaria(),
                        tema.getCorSecundaria()))
                .orElseGet(() -> new TemaResponse(tenant.getNomeEmpresa(), null, null, null));
    }

    private String montarLogoUrl(Tenant tenant, TenantTema tema) {
        if (tema.getLogoArquivo() == null) {
            return null;
        }
        return "/tenants/" + tenant.getGuid() + "/" + tema.getLogoArquivo();
    }
}
