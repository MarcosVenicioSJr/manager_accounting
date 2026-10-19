package br.com.manageraccounting.modules.cliente;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Geração e hash do token que o cliente digita no app.
 * <p>
 * Formato: 16 caracteres do alfabeto Base32 de Crockford (sem I, L, O e U, que confundem na digitação),
 * em 4 grupos: {@code XXXX-XXXX-XXXX-XXXX}. São 80 bits aleatórios. O hash é calculado sobre o valor
 * normalizado (maiúsculo, sem hífens e espaços), então o cliente pode digitar com ou sem os hífens.
 */
public final class TokenAcessoApp {

    static final String ALFABETO = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";
    static final int TAMANHO = 16;

    private static final SecureRandom RANDOM = new SecureRandom();

    private TokenAcessoApp() {
    }

    /** Novo token já formatado para exibição (com hífens). */
    public static String gerar() {
        StringBuilder token = new StringBuilder(TAMANHO + 3);
        for (int i = 0; i < TAMANHO; i++) {
            if (i > 0 && i % 4 == 0) {
                token.append('-');
            }
            token.append(ALFABETO.charAt(RANDOM.nextInt(ALFABETO.length())));
        }
        return token.toString();
    }

    /**
     * Como o cliente digitou → como o token é comparado. Segue as trocas do Crockford: O vira 0, I e L viram 1.
     */
    public static String normalizar(String digitado) {
        if (digitado == null) {
            return "";
        }
        return digitado.toUpperCase(Locale.ROOT)
                .replaceAll("[\\s-]", "")
                .replace('O', '0')
                .replace('I', '1')
                .replace('L', '1');
    }

    /** SHA-256 (hex) do token normalizado. */
    public static String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(normalizar(token).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponível", ex);
        }
    }
}
