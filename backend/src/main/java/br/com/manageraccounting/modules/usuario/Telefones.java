package br.com.manageraccounting.modules.usuario;

import br.com.manageraccounting.core.exception.BusinessException;

/** Telefone do usuário: guarda só os dígitos (DDD + número, com ou sem código do país). */
public final class Telefones {

    private Telefones() {
    }

    public static String normalizar(String telefone) {
        String digitos = telefone == null ? "" : telefone.replaceAll("\\D", "");
        if (digitos.length() < 10 || digitos.length() > 13) {
            throw new BusinessException("Telefone inválido. Informe o DDD e o número.");
        }
        return digitos;
    }
}
