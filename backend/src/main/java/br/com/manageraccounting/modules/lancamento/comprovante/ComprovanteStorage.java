package br.com.manageraccounting.modules.lancamento.comprovante;

import java.io.IOException;
import java.io.InputStream;

/**
 * Onde os arquivos de comprovante ficam. Hoje em disco ({@link DiscoComprovanteStorage}); em produção a ideia é
 * trocar por S3 ou compatível sem mexer no serviço de lançamentos.
 */
public interface ComprovanteStorage {

    /**
     * Grava o conteúdo e devolve a chave gerada.
     *
     * @param tenantId usado para separar os arquivos por escritório
     */
    String gravar(Long tenantId, TipoComprovante tipo, InputStream conteudo) throws IOException;

    InputStream abrir(String chave) throws IOException;

    /** Apaga o arquivo; não faz nada se ele já não existir. */
    void apagar(String chave) throws IOException;
}
