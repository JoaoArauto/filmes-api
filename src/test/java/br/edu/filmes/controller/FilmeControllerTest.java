package br.edu.filmes.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.filmes.dto.FilmeRequest;
import br.edu.filmes.dto.FilmeResponse;
import br.edu.filmes.exception.FilmeNaoEncontradoException;
import br.edu.filmes.service.FilmeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FilmeController.class)
class FilmeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FilmeService service;

    @Test
    void deveRetornarFilmePorIdComStatus200() throws Exception {
        FilmeResponse response = new FilmeResponse(1L, "O Auto da Compadecida", "Guel Arraes", "Comedia", 2000, 104);
        when(service.buscarPorId(1L)).thenReturn(response);

        mockMvc.perform(get("/filmes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.titulo").value("O Auto da Compadecida"))
                .andExpect(jsonPath("$.anoLancamento").value(2000));
    }

    @Test
    void deveRetornar404QuandoFilmeNaoExistir() throws Exception {
        when(service.buscarPorId(999L)).thenThrow(new FilmeNaoEncontradoException(999L));

        mockMvc.perform(get("/filmes/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void deveCadastrarFilmeComStatus201() throws Exception {
        FilmeResponse response = new FilmeResponse(2L, "Bacurau", "Kleber Mendonca Filho", "Drama", 2019, 131);
        when(service.cadastrar(any(FilmeRequest.class))).thenReturn(response);

        mockMvc.perform(post("/filmes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "titulo": "Bacurau",
                                    "diretor": "Kleber Mendonca Filho",
                                    "genero": "Drama",
                                    "anoLancamento": 2019,
                                    "duracaoMinutos": 131
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.titulo").value("Bacurau"));
    }

    @Test
    void deveRetornar400QuandoDadosForemInvalidos() throws Exception {
        mockMvc.perform(post("/filmes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "titulo": "",
                                    "diretor": "",
                                    "genero": "Acao",
                                    "anoLancamento": 1700,
                                    "duracaoMinutos": -10
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.titulo").exists())
                .andExpect(jsonPath("$.campos.duracaoMinutos").exists());
    }
}
