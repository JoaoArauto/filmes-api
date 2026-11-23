package br.edu.filmes.exception;

public class FilmeNaoEncontradoException extends RuntimeException {
    public FilmeNaoEncontradoException(Long id) {
        super("Filme nao encontrado com id: " + id);
    }
}
