package br.com.manageraccounting.modules.auth;

import java.nio.charset.StandardCharsets;

import br.com.manageraccounting.core.exception.BusinessException;

/**
 * Política de senha: mínimo de 8 caracteres, com letras, números e símbolos.
 * O BCrypt só considera os primeiros 72 bytes, por isso senhas maiores são recusadas.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_BYTES = 72;

    private PasswordPolicy() {
    }

    public static void validar(String senha) {
        if (senha == null || !atendeRequisitos(senha)) {
            throw new BusinessException(
                    "A senha deve ter no mínimo " + MIN_LENGTH + " caracteres, com letras, números e símbolos.");
        }
        if (excedeLimiteDoBcrypt(senha)) {
            throw new BusinessException("A senha é longa demais (máximo de " + MAX_BYTES + " bytes).");
        }
    }

    public static boolean excedeLimiteDoBcrypt(String senha) {
        return senha.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES;
    }

    private static boolean atendeRequisitos(String senha) {
        if (senha.length() < MIN_LENGTH) {
            return false;
        }
        boolean letra = false;
        boolean numero = false;
        boolean simbolo = false;
        for (int i = 0; i < senha.length(); i++) {
            char c = senha.charAt(i);
            if (Character.isLetter(c)) {
                letra = true;
            } else if (Character.isDigit(c)) {
                numero = true;
            } else if (!Character.isWhitespace(c)) {
                simbolo = true;
            }
        }
        return letra && numero && simbolo;
    }
}
