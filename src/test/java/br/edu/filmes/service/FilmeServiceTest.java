package br.edu.filmes.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.edu.filmes.dto.FilmeRequest;
import br.edu.filmes.dto.FilmeResponse;
import br.edu.filmes.exception.FilmeNaoEncontradoException;
import br.edu.filmes.model.Filme;
import br.edu.filmes.repository.FilmeRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FilmeServiceTest {

    @Mock
    private FilmeRepository repository;

    @InjectMocks
    private FilmeService service;

    @Test
    void deveCadastrarFilmeValido() {
        // Arrange
        FilmeRequest request = new FilmeRequest("Cidade de Deus", "Fernando Meirelles", "Drama", 2002, 130);
        Filme salvo = new Filme(1L, "Cidade de Deus", "Fernando Meirelles", "Drama", 2002, 130);
        when(repository.save(any(Filme.class))).thenReturn(salvo);

        // Act
        FilmeResponse resultado = service.cadastrar(request);

        // Assert
        assertEquals(1L, resultado.id());
        assertEquals("Cidade de Deus", resultado.titulo());
        assertEquals(130, resultado.duracaoMinutos());
        verify(repository, times(1)).save(any(Filme.class));
    }

    @Test
    void deveBuscarFilmePorIdQuandoExistir() {
        // Arrange
        Filme existente = new Filme(10L, "Parasita", "Bong Joon-ho", "Suspense", 2019, 132);
        when(repository.findById(10L)).thenReturn(Optional.of(existente));

        // Act
        FilmeResponse resultado = service.buscarPorId(10L);

        // Assert
        assertEquals(10L, resultado.id());
        assertEquals("Parasita", resultado.titulo());
        verify(repository, times(1)).findById(10L);
    }

    @Test
    void deveAtualizarFilmeExistente() {
        // Arrange
        Filme existente = new Filme(5L, "Matrix", "Lana Wachowski", "Ficcao", 1999, 136);
        FilmeRequest novo = new FilmeRequest("Matrix", "Lana e Lilly Wachowski", "Ficcao Cientifica", 1999, 136);
        when(repository.findById(5L)).thenReturn(Optional.of(existente));
        when(repository.save(any(Filme.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        FilmeResponse resultado = service.atualizar(5L, novo);

        // Assert
        assertEquals("Lana e Lilly Wachowski", resultado.diretor());
        assertEquals("Ficcao Cientifica", resultado.genero());
        verify(repository, times(1)).save(existente);
    }

    @Test
    void deveLancarErroQuandoBuscarFilmeInexistente() {
        // Arrange
        when(repository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(FilmeNaoEncontradoException.class, () -> service.buscarPorId(999L));
        verify(repository, times(1)).findById(999L);
    }

    @Test
    void deveLancarErroAoAtualizarFilmeInexistente() {
        // Arrange
        FilmeRequest request = new FilmeRequest("Interestelar", "Christopher Nolan", "Ficcao", 2014, 169);
        when(repository.findById(88L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(FilmeNaoEncontradoException.class, () -> service.atualizar(88L, request));
        verify(repository, never()).save(any(Filme.class));
    }

    @Test
    void deveLancarErroAoExcluirFilmeInexistente() {
        // Arrange
        when(repository.findById(77L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(FilmeNaoEncontradoException.class, () -> service.excluir(77L));
        verify(repository, never()).delete(any(Filme.class));
    }
}
