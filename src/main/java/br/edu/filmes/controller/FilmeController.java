package br.edu.filmes.controller;

import br.edu.filmes.dto.FilmeRequest;
import br.edu.filmes.dto.FilmeResponse;
import br.edu.filmes.service.FilmeService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/filmes")
public class FilmeController {

    private final FilmeService service;

    public FilmeController(FilmeService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FilmeResponse criar(@Valid @RequestBody FilmeRequest body) {
        return service.cadastrar(body);
    }

    @GetMapping
    public List<FilmeResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public FilmeResponse buscar(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @PutMapping("/{id}")
    public FilmeResponse atualizar(@PathVariable Long id, @Valid @RequestBody FilmeRequest body) {
        return service.atualizar(id, body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        service.excluir(id);
    }
}
