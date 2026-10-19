package br.com.manageraccounting.modules.cliente.dto;

/** Números da página inicial do backoffice. */
public record ClientesResumoResponse(long ativos, long inativos, long comAcessoApp) {
}
