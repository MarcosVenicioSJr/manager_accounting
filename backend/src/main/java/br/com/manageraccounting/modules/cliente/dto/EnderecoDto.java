package br.com.manageraccounting.modules.cliente.dto;

import jakarta.validation.constraints.Size;

/**
 * Endereço do cliente. Todos os campos são opcionais. CEP só com dígitos (8) ou com máscara; UF com 2 letras.
 */
public record EnderecoDto(
        @Size(max = 9, message = "CEP inválido")
        String cep,

        @Size(max = 150, message = "Logradouro muito longo (máximo de 150 caracteres)")
        String logradouro,

        @Size(max = 20, message = "Número muito longo (máximo de 20 caracteres)")
        String numero,

        @Size(max = 100, message = "Complemento muito longo (máximo de 100 caracteres)")
        String complemento,

        @Size(max = 100, message = "Bairro muito longo (máximo de 100 caracteres)")
        String bairro,

        @Size(max = 100, message = "Cidade muito longa (máximo de 100 caracteres)")
        String cidade,

        @Size(max = 2, message = "UF inválida")
        String uf) {
}
