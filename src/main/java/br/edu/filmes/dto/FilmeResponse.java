package br.edu.filmes.dto;

import br.edu.filmes.model.Filme;

public record FilmeResponse(
        Long id,
        String titulo,
        String diretor,
        String genero,
        Integer anoLancamento,
        Integer duracaoMinutos
) {
    public static FilmeResponse from(Filme filme) {
        return new FilmeResponse(
                filme.getId(),
                filme.getTitulo(),
                filme.getDiretor(),
                filme.getGenero(),
                filme.getAnoLancamento(),
                filme.getDuracaoMinutos()
        );
    }
}
