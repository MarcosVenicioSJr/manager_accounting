package br.com.manageraccounting.modules.dashboard;

import java.time.ZoneId;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param fusoHorario fuso usado para dizer em que mês um cliente foi cadastrado (o banco guarda tudo em UTC)
 */
@ConfigurationProperties(prefix = "app.dashboard")
public record DashboardProperties(@DefaultValue("America/Sao_Paulo") ZoneId fusoHorario) {
}
