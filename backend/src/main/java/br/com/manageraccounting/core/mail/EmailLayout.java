package br.com.manageraccounting.core.mail;

import org.springframework.web.util.HtmlUtils;

/**
 * Moldura comum dos e-mails do sistema: visual genérico ("Backoffice contábil"), saudação com o primeiro nome
 * e escape de HTML. Usada pela recuperação de senha e pelo Meu perfil.
 */
public final class EmailLayout {

    public static final String COR_PRIMARIA = "#1A6BDC";
    public static final String COR_TEXTO = "#1f2937";
    public static final String COR_SUAVE = "#6b7280";

    private EmailLayout() {
    }

    /** "Olá, Maria!" usando só o primeiro nome. */
    public static String saudacao(String nome) {
        if (nome == null || nome.isBlank()) {
            return "Olá!";
        }
        return "Olá, " + nome.trim().split("\\s+")[0] + "!";
    }

    public static String layout(String titulo, String corpo) {
        return """
                <!doctype html>
                <html lang="pt-BR">
                <head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
                <title>%s</title></head>
                <body style="margin:0;padding:0;background:#f3f5f9">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#f3f5f9">
                <tr><td align="center" style="padding:32px 16px">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0"
                style="max-width:520px;background:#ffffff;border-radius:12px;font-family:Arial,Helvetica,sans-serif;
                font-size:15px;line-height:1.5;color:%s">
                <tr><td style="background:#0e2140;border-radius:12px 12px 0 0;padding:20px 32px;color:#ffffff;
                font-weight:700;font-size:16px">Backoffice contábil</td></tr>
                <tr><td style="padding:32px">
                <h1 style="margin:0 0 20px;font-size:20px;color:%s">%s</h1>
                %s
                </td></tr>
                </table>
                <p style="margin:16px 0 0;font-family:Arial,Helvetica,sans-serif;font-size:12px;color:%s">
                E-mail automático. Não é preciso responder.</p>
                </td></tr>
                </table>
                </body>
                </html>
                """.formatted(esc(titulo), COR_TEXTO, COR_TEXTO, esc(titulo), corpo, COR_SUAVE);
    }

    public static String esc(String valor) {
        return HtmlUtils.htmlEscape(valor == null ? "" : valor, "UTF-8");
    }
}
