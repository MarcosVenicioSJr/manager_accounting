package br.com.manageraccounting.modules.auth;

import org.springframework.web.util.HtmlUtils;

import br.com.manageraccounting.core.mail.EmailMessage;

/**
 * Textos dos e-mails da recuperação de senha. Visual genérico ("Backoffice contábil"), citando o nome do
 * escritório (tenant) para a pessoa reconhecer de onde vem o e-mail.
 */
final class RecuperacaoSenhaEmails {

    static final String ASSUNTO_LINK = "Redefinição de senha";
    static final String ASSUNTO_CONFIRMACAO = "Sua senha foi alterada";

    private static final String COR_PRIMARIA = "#1A6BDC";
    private static final String COR_TEXTO = "#1f2937";
    private static final String COR_SUAVE = "#6b7280";

    private RecuperacaoSenhaEmails() {
    }

    static EmailMessage link(String para, String nome, String nomeEmpresa, String link, long validadeMinutos) {
        String saudacao = saudacao(nome);
        String texto = """
                %s

                Recebemos um pedido para redefinir a senha do seu acesso ao backoffice de %s.

                Para criar uma nova senha, abra o link abaixo (vale por %d minutos e só pode ser usado uma vez):
                %s

                Se você não fez esse pedido, ignore este e-mail. Sua senha atual continua valendo.

                Backoffice contábil
                """.formatted(saudacao, nomeEmpresa, validadeMinutos, link);

        String corpo = """
                <p style="margin:0 0 16px">%s</p>
                <p style="margin:0 0 16px">Recebemos um pedido para redefinir a senha do seu acesso ao backoffice de
                <strong>%s</strong>.</p>
                <p style="margin:0 0 24px">Para criar uma nova senha, clique no botão abaixo. O link vale por
                <strong>%d minutos</strong> e só pode ser usado uma vez.</p>
                <p style="margin:0 0 24px">
                  <a href="%s" style="display:inline-block;background:%s;color:#ffffff;text-decoration:none;
                  font-weight:600;padding:12px 24px;border-radius:8px">Redefinir senha</a>
                </p>
                <p style="margin:0 0 8px;color:%s;font-size:13px">Se o botão não funcionar, copie e cole este endereço
                no navegador:</p>
                <p style="margin:0 0 24px;font-size:13px;word-break:break-all"><a href="%s" style="color:%s">%s</a></p>
                <p style="margin:0;color:%s;font-size:13px">Se você não fez esse pedido, ignore este e-mail. Sua senha
                atual continua valendo.</p>
                """.formatted(
                esc(saudacao), esc(nomeEmpresa), validadeMinutos,
                esc(link), COR_PRIMARIA,
                COR_SUAVE,
                esc(link), COR_PRIMARIA, esc(link),
                COR_SUAVE);

        return new EmailMessage(para, ASSUNTO_LINK, texto, layout(ASSUNTO_LINK, corpo));
    }

    static EmailMessage confirmacao(String para, String nome, String nomeEmpresa, String linkLogin) {
        String saudacao = saudacao(nome);
        String texto = """
                %s

                A senha do seu acesso ao backoffice de %s acabou de ser alterada pelo "Esqueceu sua senha?".
                Por segurança, as sessões abertas em outros navegadores foram encerradas.

                Se foi você, está tudo certo. Entre com a nova senha em: %s

                Se não foi você, avise o seu escritório imediatamente.

                Backoffice contábil
                """.formatted(saudacao, nomeEmpresa, linkLogin);

        String corpo = """
                <p style="margin:0 0 16px">%s</p>
                <p style="margin:0 0 16px">A senha do seu acesso ao backoffice de <strong>%s</strong> acabou de ser
                alterada pelo "Esqueceu sua senha?". Por segurança, as sessões abertas em outros navegadores foram
                encerradas.</p>
                <p style="margin:0 0 24px">Se foi você, está tudo certo:
                <a href="%s" style="color:%s">entrar com a nova senha</a>.</p>
                <p style="margin:0;color:%s;font-size:13px">Se não foi você, avise o seu escritório imediatamente.</p>
                """.formatted(esc(saudacao), esc(nomeEmpresa), esc(linkLogin), COR_PRIMARIA, COR_SUAVE);

        return new EmailMessage(para, ASSUNTO_CONFIRMACAO, texto, layout(ASSUNTO_CONFIRMACAO, corpo));
    }

    /** "Olá, Maria!" usando só o primeiro nome. */
    private static String saudacao(String nome) {
        if (nome == null || nome.isBlank()) {
            return "Olá!";
        }
        return "Olá, " + nome.trim().split("\\s+")[0] + "!";
    }

    private static String layout(String titulo, String corpo) {
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

    private static String esc(String valor) {
        return HtmlUtils.htmlEscape(valor == null ? "" : valor, "UTF-8");
    }
}
