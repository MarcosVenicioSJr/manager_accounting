package br.com.manageraccounting.modules.cliente.dto;

import java.time.Instant;

/** Situação do acesso do cliente ao app. O token em si nunca é devolvido depois de gerado. */
public record AcessoAppResponse(boolean ativo, Instant geradoEm, Instant ultimoUsoEm) {

    public static AcessoAppResponse semAcesso() {
        return new AcessoAppResponse(false, null, null);
    }
}
