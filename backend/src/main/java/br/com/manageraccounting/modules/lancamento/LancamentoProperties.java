package br.com.manageraccounting.modules.lancamento;

import java.time.ZoneId;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param fusoHorario fuso usado para saber que dia é "hoje" (a data do lançamento não pode ser no futuro)
 */
@ConfigurationProperties(prefix = "app.lancamentos")
public record LancamentoProperties(@DefaultValue("America/Sao_Paulo") ZoneId fusoHorario) {
}
