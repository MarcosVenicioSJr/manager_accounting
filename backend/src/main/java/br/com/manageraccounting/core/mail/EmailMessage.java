package br.com.manageraccounting.core.mail;

/**
 * E-mail pronto para envio, com versão em texto e em HTML (o cliente de e-mail escolhe qual mostrar).
 */
public record EmailMessage(String para, String assunto, String texto, String html) {
}
