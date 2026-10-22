package br.com.manageraccounting.modules.perfil;

import static br.com.manageraccounting.core.mail.EmailLayout.COR_PRIMARIA;
import static br.com.manageraccounting.core.mail.EmailLayout.COR_SUAVE;
import static br.com.manageraccounting.core.mail.EmailLayout.esc;
import static br.com.manageraccounting.core.mail.EmailLayout.layout;
import static br.com.manageraccounting.core.mail.EmailLayout.saudacao;

import br.com.manageraccounting.core.mail.EmailMessage;

/**
 * Textos dos e-mails do Meu perfil: confirmação da troca de senha, confirmação do novo e-mail e os avisos
 * depois que a troca acontece.
 */
final class PerfilEmails {

    static final String ASSUNTO_CONFIRMAR_SENHA = "Confirme a troca de senha";
    static final String ASSUNTO_SENHA_ALTERADA = "Sua senha foi alterada";
    static final String ASSUNTO_CONFIRMAR_EMAIL = "Confirme seu novo e-mail";
    static final String ASSUNTO_EMAIL_ALTERADO = "Seu e-mail foi alterado";

    private PerfilEmails() {
    }

    /** Vai para o e-mail atual: a troca só acontece se a pessoa confirmar. */
    static EmailMessage confirmarSenha(String para, String nome, String nomeEmpresa, String link, long minutos) {
        String saudacao = saudacao(nome);
        String texto = """
                %s

                Recebemos um pedido para trocar a senha do seu acesso ao backoffice de %s, feito no Meu perfil.
                A troca só acontece depois que você confirmar.

                Para confirmar, abra o link abaixo (vale por %d minutos e só pode ser usado uma vez):
                %s

                Se não foi você, não abra o link: a senha atual continua valendo. Como o pedido exige a senha
                atual, recomendamos trocá-la e avisar o seu escritório.

                Backoffice contábil
                """.formatted(saudacao, nomeEmpresa, minutos, link);

        String corpo = """
                <p style="margin:0 0 16px">%s</p>
                <p style="margin:0 0 16px">Recebemos um pedido para trocar a senha do seu acesso ao backoffice de
                <strong>%s</strong>, feito no Meu perfil. A troca só acontece depois que você confirmar.</p>
                <p style="margin:0 0 24px">O link vale por <strong>%d minutos</strong> e só pode ser usado uma vez.</p>
                %s
                <p style="margin:0;color:%s;font-size:13px">Se não foi você, não abra o link: a senha atual continua
                valendo. Como o pedido exige a senha atual, recomendamos trocá-la e avisar o seu escritório.</p>
                """.formatted(esc(saudacao), esc(nomeEmpresa), minutos, botao(link, "Confirmar troca de senha"),
                COR_SUAVE);

        return new EmailMessage(para, ASSUNTO_CONFIRMAR_SENHA, texto, layout(ASSUNTO_CONFIRMAR_SENHA, corpo));
    }

    static EmailMessage senhaAlterada(String para, String nome, String nomeEmpresa, String linkLogin) {
        String saudacao = saudacao(nome);
        String texto = """
                %s

                A senha do seu acesso ao backoffice de %s foi alterada pelo Meu perfil.
                Por segurança, todas as sessões abertas foram encerradas. Entre de novo com a nova senha em: %s

                Se não foi você, avise o seu escritório imediatamente.

                Backoffice contábil
                """.formatted(saudacao, nomeEmpresa, linkLogin);

        String corpo = """
                <p style="margin:0 0 16px">%s</p>
                <p style="margin:0 0 16px">A senha do seu acesso ao backoffice de <strong>%s</strong> foi alterada
                pelo Meu perfil. Por segurança, todas as sessões abertas foram encerradas.</p>
                <p style="margin:0 0 24px"><a href="%s" style="color:%s">Entrar com a nova senha</a>.</p>
                <p style="margin:0;color:%s;font-size:13px">Se não foi você, avise o seu escritório imediatamente.</p>
                """.formatted(esc(saudacao), esc(nomeEmpresa), esc(linkLogin), COR_PRIMARIA, COR_SUAVE);

        return new EmailMessage(para, ASSUNTO_SENHA_ALTERADA, texto, layout(ASSUNTO_SENHA_ALTERADA, corpo));
    }

    /** Vai para o NOVO e-mail: confirmar prova que a pessoa tem acesso a ele. */
    static EmailMessage confirmarEmail(String para, String nome, String nomeEmpresa, String link, long minutos) {
        String saudacao = saudacao(nome);
        String texto = """
                %s

                Este endereço foi informado como o novo e-mail do seu acesso ao backoffice de %s.
                Ele só passa a valer depois que você confirmar.

                Para confirmar, abra o link abaixo (vale por %d minutos e só pode ser usado uma vez):
                %s

                Se você não reconhece esse pedido, ignore este e-mail.

                Backoffice contábil
                """.formatted(saudacao, nomeEmpresa, minutos, link);

        String corpo = """
                <p style="margin:0 0 16px">%s</p>
                <p style="margin:0 0 16px">Este endereço foi informado como o novo e-mail do seu acesso ao backoffice
                de <strong>%s</strong>. Ele só passa a valer depois que você confirmar.</p>
                <p style="margin:0 0 24px">O link vale por <strong>%d minutos</strong> e só pode ser usado uma vez.</p>
                %s
                <p style="margin:0;color:%s;font-size:13px">Se você não reconhece esse pedido, ignore este e-mail.</p>
                """.formatted(esc(saudacao), esc(nomeEmpresa), minutos, botao(link, "Confirmar novo e-mail"),
                COR_SUAVE);

        return new EmailMessage(para, ASSUNTO_CONFIRMAR_EMAIL, texto, layout(ASSUNTO_CONFIRMAR_EMAIL, corpo));
    }

    /** Vai para o e-mail ANTIGO, depois da troca, com o novo endereço parcialmente escondido. */
    static EmailMessage emailAlterado(String para, String nome, String nomeEmpresa, String novoEmailMascarado) {
        String saudacao = saudacao(nome);
        String texto = """
                %s

                O e-mail do seu acesso ao backoffice de %s foi trocado para %s.
                Daqui em diante, os avisos e a recuperação de senha vão para o novo endereço.

                Se não foi você, avise o seu escritório imediatamente.

                Backoffice contábil
                """.formatted(saudacao, nomeEmpresa, novoEmailMascarado);

        String corpo = """
                <p style="margin:0 0 16px">%s</p>
                <p style="margin:0 0 16px">O e-mail do seu acesso ao backoffice de <strong>%s</strong> foi trocado
                para <strong>%s</strong>. Daqui em diante, os avisos e a recuperação de senha vão para o novo
                endereço.</p>
                <p style="margin:0;color:%s;font-size:13px">Se não foi você, avise o seu escritório imediatamente.</p>
                """.formatted(esc(saudacao), esc(nomeEmpresa), esc(novoEmailMascarado), COR_SUAVE);

        return new EmailMessage(para, ASSUNTO_EMAIL_ALTERADO, texto, layout(ASSUNTO_EMAIL_ALTERADO, corpo));
    }

    /** "maria.souza@empresa.com.br" → "ma***@empresa.com.br". */
    static String mascarar(String email) {
        int arroba = email.indexOf('@');
        if (arroba <= 0) {
            return "***";
        }
        String usuario = email.substring(0, arroba);
        String visivel = usuario.length() <= 2 ? usuario.substring(0, 1) : usuario.substring(0, 2);
        return visivel + "***" + email.substring(arroba);
    }

    private static String botao(String link, String rotulo) {
        return """
                <p style="margin:0 0 24px">
                  <a href="%s" style="display:inline-block;background:%s;color:#ffffff;text-decoration:none;
                  font-weight:600;padding:12px 24px;border-radius:8px">%s</a>
                </p>
                <p style="margin:0 0 8px;color:%s;font-size:13px">Se o botão não funcionar, copie e cole este endereço
                no navegador:</p>
                <p style="margin:0 0 24px;font-size:13px;word-break:break-all"><a href="%s" style="color:%s">%s</a></p>
                """.formatted(esc(link), COR_PRIMARIA, esc(rotulo), COR_SUAVE, esc(link), COR_PRIMARIA, esc(link));
    }
}
