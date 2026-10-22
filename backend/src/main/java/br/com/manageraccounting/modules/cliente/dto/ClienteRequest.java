package br.com.manageraccounting.modules.cliente.dto;

import br.com.manageraccounting.modules.cliente.RegimeTributario;
import br.com.manageraccounting.modules.cliente.TipoPessoa;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Cadastro e edição de cliente. O tenant nunca vem aqui: é sempre o do token do contador.
 * Na edição, {@code versao} é obrigatória (a versão que a tela carregou) para não sobrescrever a alteração
 * de outra pessoa.
 */
public record ClienteRequest(
        @NotNull(message = "Informe se é pessoa física ou jurídica")
        TipoPessoa tipoPessoa,

        @NotBlank(message = "Informe o CPF ou CNPJ")
        @Size(max = 20, message = "CPF/CNPJ muito longo")
        String documento,

        @NotBlank(message = "Informe o nome")
        @Size(max = 150, message = "Nome muito longo (máximo de 150 caracteres)")
        String nome,

        @Size(max = 150, message = "Nome fantasia muito longo (máximo de 150 caracteres)")
        String nomeFantasia,

        @Email(message = "E-mail inválido")
        @Size(max = 150, message = "E-mail muito longo")
        String email,

        @Size(max = 20, message = "Telefone muito longo")
        String telefone,

        RegimeTributario regimeTributario,

        @Size(max = 1000, message = "Observações muito longas (máximo de 1000 caracteres)")
        String observacoes,

        Long versao,

        @Valid
        EnderecoDto endereco) {
}
