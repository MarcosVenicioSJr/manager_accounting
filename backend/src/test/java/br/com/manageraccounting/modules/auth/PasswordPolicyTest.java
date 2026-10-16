package br.com.manageraccounting.modules.auth;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import br.com.manageraccounting.core.exception.BusinessException;

class PasswordPolicyTest {

    @Test
    void aceitaSenhaComOitoCaracteresLetrasNumerosESimbolos() {
        assertThatCode(() -> PasswordPolicy.validar("Abcd@123")).doesNotThrowAnyException();
    }

    @Test
    void recusaSenhaCurta() {
        assertThatThrownBy(() -> PasswordPolicy.validar("Ab@1")).isInstanceOf(BusinessException.class);
    }

    @Test
    void recusaSenhaSemLetra() {
        assertThatThrownBy(() -> PasswordPolicy.validar("1234@5678")).isInstanceOf(BusinessException.class);
    }

    @Test
    void recusaSenhaSemNumero() {
        assertThatThrownBy(() -> PasswordPolicy.validar("Abcdef@gh")).isInstanceOf(BusinessException.class);
    }

    @Test
    void recusaSenhaSemSimbolo() {
        assertThatThrownBy(() -> PasswordPolicy.validar("Abcdef123")).isInstanceOf(BusinessException.class);
    }

    @Test
    void recusaSenhaMaiorQueOLimiteDoBcrypt() {
        String longa = "Ab@1" + "x".repeat(70);

        assertThatThrownBy(() -> PasswordPolicy.validar(longa)).isInstanceOf(BusinessException.class);
    }
}
