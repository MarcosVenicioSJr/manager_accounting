package br.com.manageraccounting.modules.cliente.dto;

import java.util.List;

/** Página de resultados. {@code pagina} começa em 0. */
public record PaginaResponse<T>(List<T> itens, int pagina, int tamanho, long total, int totalPaginas) {
}
