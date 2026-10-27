package br.com.manageraccounting.modules.lancamento.comprovante;

import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.unit.DataSize;

/**
 * @param diretorio     pasta onde os comprovantes ficam gravados (uma subpasta por escritório)
 * @param tamanhoMaximo tamanho máximo de cada arquivo
 */
@ConfigurationProperties(prefix = "app.lancamentos.comprovante")
public record ComprovanteProperties(
        @DefaultValue("./data/comprovantes") Path diretorio,
        @DefaultValue("10MB") DataSize tamanhoMaximo) {
}
