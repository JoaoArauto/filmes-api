package br.edu.filmes.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record FilmeRequest(
        @NotBlank(message = "O titulo e obrigatorio")
        String titulo,

        @NotBlank(message = "O diretor e obrigatorio")
        String diretor,

        @NotBlank(message = "O genero e obrigatorio")
        String genero,

        @NotNull(message = "O ano de lancamento e obrigatorio")
        @Min(value = 1888, message = "O ano de lancamento deve ser a partir de 1888")
        @Max(value = 2100, message = "O ano de lancamento deve ser valido")
        Integer anoLancamento,

        @NotNull(message = "A duracao e obrigatoria")
        @Positive(message = "A duracao deve ser maior que zero")
        Integer duracaoMinutos
) {
}
