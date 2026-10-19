package br.com.manageraccounting.modules.cliente;

import java.util.Locale;
import java.util.regex.Pattern;

import br.com.manageraccounting.core.exception.BusinessException;

/**
 * Validação e normalização de CPF e CNPJ.
 * <p>
 * O CNPJ aceita o formato alfanumérico da Receita Federal (em vigor desde julho de 2026): 12 posições com letras
 * maiúsculas ou números e 2 dígitos verificadores numéricos. O cálculo é o mesmo módulo 11 de sempre, usando para
 * cada caractere o valor do código ASCII menos 48 (números continuam valendo 0 a 9; "A" vale 17, "Z" vale 42).
 * CNPJs só com números continuam válidos.
 * <p>
 * O documento é guardado sem pontuação e em maiúsculas.
 */
public final class DocumentoFiscal {

    private static final Pattern CPF = Pattern.compile("\\d{11}");
    private static final Pattern CNPJ = Pattern.compile("[0-9A-Z]{12}\\d{2}");
    private static final Pattern PONTUACAO = Pattern.compile("[\\s./-]");

    private static final int[] PESOS_CNPJ_DV1 = { 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2 };
    private static final int[] PESOS_CNPJ_DV2 = { 6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2 };

    private DocumentoFiscal() {
    }

    /** Devolve o documento sem pontuação, ou lança {@link BusinessException} se for inválido para o tipo. */
    public static String normalizar(String documento, TipoPessoa tipo) {
        String limpo = documento == null ? "" : PONTUACAO.matcher(documento).replaceAll("").toUpperCase(Locale.ROOT);
        if (tipo == TipoPessoa.FISICA) {
            if (!cpfValido(limpo)) {
                throw new BusinessException("CPF inválido.");
            }
        } else if (!cnpjValido(limpo)) {
            throw new BusinessException("CNPJ inválido.");
        }
        return limpo;
    }

    static boolean cpfValido(String cpf) {
        if (!CPF.matcher(cpf).matches() || todosIguais(cpf)) {
            return false;
        }
        return digitoCpf(cpf, 9) == cpf.charAt(9) - '0' && digitoCpf(cpf, 10) == cpf.charAt(10) - '0';
    }

    static boolean cnpjValido(String cnpj) {
        if (!CNPJ.matcher(cnpj).matches() || todosIguais(cnpj)) {
            return false;
        }
        return digitoCnpj(cnpj, PESOS_CNPJ_DV1) == cnpj.charAt(12) - '0'
                && digitoCnpj(cnpj, PESOS_CNPJ_DV2) == cnpj.charAt(13) - '0';
    }

    private static int digitoCpf(String cpf, int tamanho) {
        int soma = 0;
        for (int i = 0; i < tamanho; i++) {
            soma += (cpf.charAt(i) - '0') * (tamanho + 1 - i);
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    private static int digitoCnpj(String cnpj, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) {
            soma += (cnpj.charAt(i) - '0') * pesos[i];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    private static boolean todosIguais(String valor) {
        return valor.chars().allMatch(c -> c == valor.charAt(0));
    }
}
