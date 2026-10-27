package br.com.manageraccounting.modules.lancamento.dto;

/** Categoria fixa: {@code valor} vai na API, {@code rotulo} aparece na tela. */
public record CategoriaResponse(String valor, String rotulo) {
}
