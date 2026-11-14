package Arthur.OurMovies;

import Arthur.OurMovies.client.TmdbClient;
import Arthur.OurMovies.dto.FilmeResponse;
import Arthur.OurMovies.exception.ApiException;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @Transactional
class ApiIntegrationTests {
    @Autowired MockMvc mvc;
    @MockitoBean TmdbClient tmdb;
    @BeforeEach
    void configurarTmdb() {
        when(tmdb.detalhes(550L)).thenReturn(new FilmeResponse(null, 550L, "Clube da Luta", "Sinopse",
                "/poster.jpg", LocalDate.of(1999, 10, 15), List.of("Drama")));
    }
    private record Item(long id, long filmeId) {}
    private Item adicionar(String status) throws Exception {
        var resposta = mvc.perform(post("/api/lista/550").contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"" + status + "\"}"))
                .andExpect(status().isCreated()).andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.filme.generos[0]").value("Drama"))
                .andExpect(jsonPath("$.mediaAvaliacoes").isEmpty()).andReturn().getResponse().getContentAsString();
        return new Item(((Number) JsonPath.read(resposta, "$.id")).longValue(),
                ((Number) JsonPath.read(resposta, "$.filme.id")).longValue());
    }
    private String avaliacao(long filme, long usuario, String nota) {
        return "{\"filmeId\":" + filme + ",\"usuarioId\":" + usuario + ",\"nota\":" + nota + ",\"comentario\":\"Muito bom\"}";
    }
    @Test
    void fluxoCompletoComDuasNotasAtualizacaoRemocaoEMedia() throws Exception {
        var item = adicionar("ASSISTIDO");
        var resposta = mvc.perform(post("/api/avaliacoes").contentType(MediaType.APPLICATION_JSON)
                .content(avaliacao(item.filmeId(), 1, "8"))).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(resposta, "$.id")).longValue();
        mvc.perform(post("/api/avaliacoes").contentType(MediaType.APPLICATION_JSON)
                .content(avaliacao(item.filmeId(), 2, "9.5"))).andExpect(status().isCreated());
        mvc.perform(get("/api/avaliacoes/filme/{id}", item.filmeId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.media").value(8.75)).andExpect(jsonPath("$.total").value(2));
        mvc.perform(put("/api/avaliacoes/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nota\":10,\"comentario\":null}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nota").value(10));
        mvc.perform(get("/api/lista")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].mediaAvaliacoes").value(9.75));
        mvc.perform(delete("/api/avaliacoes/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/avaliacoes/filme/{id}", item.filmeId())).andExpect(jsonPath("$.media").value(9.5));
        mvc.perform(delete("/api/lista/{id}", item.id())).andExpect(status().isNoContent());
        mvc.perform(get("/api/lista")).andExpect(jsonPath("$").isEmpty());
        mvc.perform(get("/api/avaliacoes/filme/{id}", item.filmeId())).andExpect(jsonPath("$.total").value(0));
        adicionar("QUERO_ASSISTIR");
        verify(tmdb, times(1)).detalhes(550L);
    }
    @Test
    void rejeitaFilmeENotaDuplicados() throws Exception {
        var item = adicionar("ASSISTIDO");
        mvc.perform(post("/api/lista/550")).andExpect(status().isConflict());
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/api/avaliacoes").contentType(MediaType.APPLICATION_JSON)
                    .content(avaliacao(item.filmeId(), 1, "0")))
                    .andExpect(i == 0 ? status().isCreated() : status().isConflict());
        }
    }
    @Test
    void exigeAssistidoEBloqueiaRetornoComAvaliacoes() throws Exception {
        var item = adicionar("QUERO_ASSISTIR");
        mvc.perform(post("/api/avaliacoes").contentType(MediaType.APPLICATION_JSON)
                .content(avaliacao(item.filmeId(), 1, "8"))).andExpect(status().isConflict());
        mvc.perform(patch("/api/lista/{id}", item.id()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"ASSISTIDO\",\"favorito\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.dataAssistido").isNotEmpty());
        mvc.perform(post("/api/avaliacoes").contentType(MediaType.APPLICATION_JSON)
                .content(avaliacao(item.filmeId(), 1, "8"))).andExpect(status().isCreated());
        mvc.perform(patch("/api/lista/{id}", item.id()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"QUERO_ASSISTIR\"}")).andExpect(status().isConflict());
    }
    @Test
    void patchPreservaCamposERetornoSemNotasLimpaData() throws Exception {
        var item = adicionar("ASSISTIDO");
        mvc.perform(patch("/api/lista/{id}", item.id()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"favorito\":true}"))
                .andExpect(jsonPath("$.status").value("ASSISTIDO")).andExpect(jsonPath("$.favorito").value(true));
        mvc.perform(patch("/api/lista/{id}", item.id()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"QUERO_ASSISTIR\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.dataAssistido").isEmpty())
                .andExpect(jsonPath("$.favorito").value(true));
    }
    @Test
    void validaNotasDatasEParametros() throws Exception {
        var item = adicionar("ASSISTIDO");
        for (String nota : List.of("-1", "10.1", "8.55", "null")) {
            mvc.perform(post("/api/avaliacoes").contentType(MediaType.APPLICATION_JSON)
                    .content(avaliacao(item.filmeId(), 1, nota))).andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/lista/550").contentType(MediaType.APPLICATION_JSON)
                .content("{\"dataAssistido\":\"2999-01-01\"}")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/filmes/pesquisar").param("nome", " ")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/filmes/pesquisar").param("nome", "Filme").param("pagina", "0")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/filmes/abc/detalhes")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/filmes/-1/detalhes")).andExpect(status().isBadRequest());
    }
    @Test
    void retorna404ParaRecursosInexistentes() throws Exception {
        mvc.perform(delete("/api/lista/999999")).andExpect(status().isNotFound());
        mvc.perform(delete("/api/avaliacoes/999999")).andExpect(status().isNotFound());
        mvc.perform(get("/api/avaliacoes/filme/999999")).andExpect(status().isNotFound());
        var item = adicionar("ASSISTIDO");
        mvc.perform(post("/api/avaliacoes").contentType(MediaType.APPLICATION_JSON)
                .content(avaliacao(item.filmeId(), 3, "8"))).andExpect(status().isNotFound());
    }
    @Test
    void usuariosECors() throws Exception {
        mvc.perform(get("/api/usuarios")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(options("/api/lista/1").header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "PATCH").header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }
    @Test
    void propagaFalhasTmdbSemPersistirFilme() throws Exception {
        when(tmdb.detalhes(550L)).thenThrow(ApiException.tmdb());
        mvc.perform(post("/api/lista/550")).andExpect(status().isBadGateway());
        mvc.perform(get("/api/lista")).andExpect(jsonPath("$").isEmpty());
    }
}
