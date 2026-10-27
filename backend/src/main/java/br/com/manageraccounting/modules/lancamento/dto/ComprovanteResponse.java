package br.com.manageraccounting.modules.lancamento.dto;

/** Dados do comprovante anexado (o arquivo em si vem de {@code GET /api/lancamentos/{id}/comprovante}). */
public record ComprovanteResponse(String nome, String tipo, long tamanho) {
}
