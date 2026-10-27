package br.com.manageraccounting.modules.cliente;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Texto de busca (clientes e lançamentos): minúsculas e sem acentos, para "joao" encontrar "João".
 * O mesmo tratamento é aplicado ao termo digitado na busca.
 */
public final class TextoBusca {

    static final int TAMANHO_MAXIMO = 500;

    private TextoBusca() {
    }

    /** "João Ação" → "joao acao". Também usado na busca da descrição dos lançamentos. */
    public static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }

    /** Junta os campos pesquisáveis do cliente (nome, nome fantasia, e-mail) já normalizados. */
    static String doCliente(String... campos) {
        String junto = Stream.of(campos)
                .filter(Objects::nonNull)
                .map(String::strip)
                .filter(c -> !c.isEmpty())
                .collect(Collectors.joining(" "));
        String normalizado = normalizar(junto);
        return normalizado.length() > TAMANHO_MAXIMO ? normalizado.substring(0, TAMANHO_MAXIMO) : normalizado;
    }
}
