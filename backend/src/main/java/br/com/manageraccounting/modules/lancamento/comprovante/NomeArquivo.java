package br.com.manageraccounting.modules.lancamento.comprovante;

import java.util.Locale;

/** Limpa o nome do arquivo enviado, para exibir na tela e usar no download. */
public final class NomeArquivo {

    static final int TAMANHO_MAXIMO = 200;

    private NomeArquivo() {
    }

    /**
     * Tira pastas ("C:\fotos\nota.jpg" vira "nota.jpg"), caracteres de controle e aspas, e garante a extensão do
     * formato real do arquivo. Sem nome aproveitável, usa "comprovante.ext".
     */
    public static String limpar(String original, TipoComprovante tipo) {
        String nome = original == null ? "" : original;
        int barra = Math.max(nome.lastIndexOf('/'), nome.lastIndexOf('\\'));
        nome = nome.substring(barra + 1)
                .replaceAll("[\\p{Cntrl}\"<>|:*?]", "")
                .strip();
        String extensao = "." + tipo.getExtensao();
        String semExtensao = nome.replaceAll("\\.[A-Za-z0-9]{1,5}$", "").strip();
        if (semExtensao.isEmpty() || semExtensao.chars().allMatch(c -> c == '.')) {
            semExtensao = "comprovante";
        }
        int limite = TAMANHO_MAXIMO - extensao.length();
        if (semExtensao.length() > limite) {
            semExtensao = semExtensao.substring(0, limite).strip();
        }
        String extensaoOriginal = nome.toLowerCase(Locale.ROOT);
        // .jpeg é a mesma coisa que .jpg: mantém a que a pessoa usou
        if (tipo == TipoComprovante.JPEG && extensaoOriginal.endsWith(".jpeg") && semExtensao.length() <= limite - 1) {
            return semExtensao + ".jpeg";
        }
        return semExtensao + extensao;
    }
}
