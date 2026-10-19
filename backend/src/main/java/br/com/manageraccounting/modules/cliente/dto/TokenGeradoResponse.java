package br.com.manageraccounting.modules.cliente.dto;

import java.time.Instant;

/** Token recém-gerado. É a única resposta que traz o valor do token; depois disso ele não pode ser consultado. */
public record TokenGeradoResponse(String token, Instant geradoEm) {
}
