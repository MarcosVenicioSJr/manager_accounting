package br.com.manageraccounting.modules.auth;

import br.com.manageraccounting.core.mail.EmailLayout;
import br.com.manageraccounting.core.mail.EmailMessage;

/**
 * Textos dos e-mails da recuperação de senha. Visual genérico ("Backoffice contábil"), citando o nome do
 * escritório (tenant) para a pessoa reconhecer de onde vem o e-mail.
 */
final class RecuperacaoSenhaEmails {

    static final String ASSUNTO_LINK = "Redefinição de senha";
    static final String ASSUNTO_CONFIRMACAO = "Sua senha foi alterada";

    private static final String COR_PRIMARIA = EmailLayout.COR_PRIMARIA;
    private static final String COR_SUAVE = EmailLayout.COR_SUAVE;

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

    private static String saudacao(String nome) {
        return EmailLayout.saudacao(nome);
    }

    private static String layout(String titulo, String corpo) {
        return EmailLayout.layout(titulo, corpo);
    }

    private static String esc(String valor) {
        return EmailLayout.esc(valor);
    }
}
