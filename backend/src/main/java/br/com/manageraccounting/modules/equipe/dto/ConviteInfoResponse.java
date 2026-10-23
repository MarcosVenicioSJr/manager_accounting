package br.com.manageraccounting.modules.equipe.dto;

/** O que a tela do convite mostra antes de a pessoa criar a senha. */
public record ConviteInfoResponse(String nomeCompleto, String login, String email, String nomeEscritorio) {
}
