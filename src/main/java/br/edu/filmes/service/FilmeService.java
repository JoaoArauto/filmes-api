package br.edu.filmes.service;

import br.edu.filmes.dto.FilmeRequest;
import br.edu.filmes.dto.FilmeResponse;
import br.edu.filmes.exception.FilmeNaoEncontradoException;
import br.edu.filmes.model.Filme;
import br.edu.filmes.repository.FilmeRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FilmeService {

    private static final Logger log = LoggerFactory.getLogger(FilmeService.class);
    private final FilmeRepository repository;

    public FilmeService(FilmeRepository repository) {
        this.repository = repository;
    }

    public FilmeResponse cadastrar(FilmeRequest req) {
        Filme salvo = repository.save(new Filme(
                null, req.titulo(), req.diretor(), req.genero(), req.anoLancamento(), req.duracaoMinutos()));
        log.info("Filme {} cadastrado: {}", salvo.getId(), salvo.getTitulo());
        return FilmeResponse.from(salvo);
    }

    public List<FilmeResponse> listar() {
        List<FilmeResponse> lista = repository.findAll().stream().map(FilmeResponse::from).toList();
        log.info("Listagem retornou {} filme(s)", lista.size());
        return lista;
    }

    public FilmeResponse buscarPorId(Long id) {
        return FilmeResponse.from(buscarEntidade(id));
    }

    public FilmeResponse atualizar(Long id, FilmeRequest req) {
        Filme filme = buscarEntidade(id);
        filme.setTitulo(req.titulo());
        filme.setDiretor(req.diretor());
        filme.setGenero(req.genero());
        filme.setAnoLancamento(req.anoLancamento());
        filme.setDuracaoMinutos(req.duracaoMinutos());
        Filme salvo = repository.save(filme);
        log.info("Filme {} atualizado: {}", salvo.getId(), salvo.getTitulo());
        return FilmeResponse.from(salvo);
    }

    public void excluir(Long id) {
        Filme filme = buscarEntidade(id);
        repository.delete(filme);
        log.info("Filme {} excluido", id);
    }

    private Filme buscarEntidade(Long id) {
        return repository.findById(id).orElseThrow(() -> {
            log.warn("Filme {} nao encontrado", id);
            return new FilmeNaoEncontradoException(id);
        });
    }
}
