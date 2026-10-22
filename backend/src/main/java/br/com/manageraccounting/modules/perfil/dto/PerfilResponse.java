package br.com.manageraccounting.modules.perfil.dto;

import java.time.Instant;

/**
 * Dados do Meu perfil. {@code trocaSenhaPendente} e {@code trocaEmailPendente} existem enquanto há um link de
 * confirmação esperando o clique.
 */
public record PerfilResponse(
        String login,
        String nomeCompleto,
        String email,
        String telefone,
        String nomeEscritorio,
        TrocaPendente trocaSenhaPendente,
        TrocaPendente trocaEmailPendente) {

    /** {@code enviadoPara}: e-mail que recebeu o link. {@code novoEmail}: só na troca de e-mail. */
    public record TrocaPendente(String enviadoPara, String novoEmail, Instant expiraEm) {
    }
}
