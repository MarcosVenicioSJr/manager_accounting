package br.com.manageraccounting.modules.lancamento.comprovante;

/**
 * Comprovante já gravado no armazenamento.
 *
 * @param chave        caminho relativo dentro do armazenamento, gerado pelo sistema (nunca vem do usuário)
 * @param nomeOriginal nome do arquivo enviado, limpo, só para exibir e para o download
 */
public record ArquivoComprovante(String chave, String nomeOriginal, TipoComprovante tipo, long tamanho) {
}
