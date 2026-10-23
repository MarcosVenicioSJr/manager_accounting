package br.com.manageraccounting.modules.equipe;

import static br.com.manageraccounting.core.mail.EmailLayout.COR_PRIMARIA;
import static br.com.manageraccounting.core.mail.EmailLayout.COR_SUAVE;
import static br.com.manageraccounting.core.mail.EmailLayout.esc;
import static br.com.manageraccounting.core.mail.EmailLayout.layout;
import static br.com.manageraccounting.core.mail.EmailLayout.saudacao;

import br.com.manageraccounting.core.mail.EmailMessage;

/** E-mail do convite de usuário do escritório. */
final class EquipeEmails {

    static final String ASSUNTO_CONVITE = "Convite para o backoffice";

    private EquipeEmails() {
    }

    static EmailMessage convite(String para, String nome, String quemConvidou, String nomeEmpresa, String login,
            String link, long horas) {
        String saudacao = saudacao(nome);
        String texto = """
                %s

                %s convidou você para acessar o backoffice de %s.
                Seu usuário de acesso é: %s

                Para criar a sua senha e entrar, abra o link abaixo (vale por %d horas e só pode ser usado uma vez):
                %s

                Se você não esperava este convite, ignore este e-mail.

                Backoffice contábil
                """.formatted(saudacao, quemConvidou, nomeEmpresa, login, horas, link);

        String corpo = """
                <p style="margin:0 0 16px">%s</p>
                <p style="margin:0 0 16px"><strong>%s</strong> convidou você para acessar o backoffice de
                <strong>%s</strong>.</p>
                <p style="margin:0 0 16px">Seu usuário de acesso é: <strong>%s</strong></p>
                <p style="margin:0 0 24px">Para criar a sua senha e entrar, clique no botão abaixo. O link vale por
                <strong>%d horas</strong> e só pode ser usado uma vez.</p>
                <p style="margin:0 0 24px">
                  <a href="%s" style="display:inline-block;background:%s;color:#ffffff;text-decoration:none;
                  font-weight:600;padding:12px 24px;border-radius:8px">Criar minha senha</a>
                </p>
                <p style="margin:0 0 8px;color:%s;font-size:13px">Se o botão não funcionar, copie e cole este endereço
                no navegador:</p>
                <p style="margin:0 0 24px;font-size:13px;word-break:break-all"><a href="%s" style="color:%s">%s</a></p>
                <p style="margin:0;color:%s;font-size:13px">Se você não esperava este convite, ignore este e-mail.</p>
                """.formatted(esc(saudacao), esc(quemConvidou), esc(nomeEmpresa), esc(login), horas,
                esc(link), COR_PRIMARIA, COR_SUAVE, esc(link), COR_PRIMARIA, esc(link), COR_SUAVE);

        return new EmailMessage(para, ASSUNTO_CONVITE, texto, layout(ASSUNTO_CONVITE, corpo));
    }
}
