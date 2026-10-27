package br.com.manageraccounting.modules.lancamento;

/**
 * Estado do lançamento guardado no histórico (coluna JSONB {@code dados}). Tudo em texto, para o JSON não depender
 * de configuração de datas e números: data em ISO (yyyy-MM-dd), valor com ponto e duas casas.
 *
 * @param comprovante nome do arquivo anexado, ou nulo
 */
public record SnapshotLancamento(String tipo, String data, String descricao, String valor, String categoria,
        String comprovante) {

    static SnapshotLancamento de(Lancamento l) {
        return new SnapshotLancamento(l.getTipo().name(), l.getData().toString(), l.getDescricao(),
                l.getValor().toPlainString(), l.getCategoria().name(), l.getComprovanteNome());
    }
}
