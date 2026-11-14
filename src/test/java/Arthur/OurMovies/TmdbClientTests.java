package Arthur.OurMovies;

import Arthur.OurMovies.client.TmdbClient;
import Arthur.OurMovies.exception.ApiException;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class TmdbClientTests {
    MockRestServiceServer server;
    TmdbClient client;
    @BeforeEach void setup() {
        var builder = RestClient.builder().baseUrl("https://api.themoviedb.org/3");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new TmdbClient(builder.build(), "test-key");
    }
    @AfterEach void verify() { server.verify(); }
    @Test void detalhesMapeiaDadosEDataVazia() {
        server.expect(requestTo("https://api.themoviedb.org/3/movie/550?api_key=test-key&language=pt-BR"))
                .andRespond(withSuccess("""
                        {"id":550,"title":"Filme","overview":"Sinopse","poster_path":null,
                         "release_date":"","genres":[{"id":18,"name":"Drama"}],"extra":"ignorado"}
                        """, MediaType.APPLICATION_JSON));
        var filme = client.detalhes(550L);
        assertThat(filme.tmdbId()).isEqualTo(550L);
        assertThat(filme.generos()).containsExactly("Drama");
        assertThat(filme.dataLancamento()).isNull();
    }
    @Test void pesquisaTraduzGenerosEPreservaPaginacao() {
        server.expect(queryParam("query", "Clube%20da%20Luta"))
                .andExpect(queryParam("page", "2"))
                .andRespond(withSuccess("""
                        {"page":2,"total_pages":3,"total_results":42,"results":[
                        {"id":550,"title":"Clube da Luta","release_date":"1999-10-15","genre_ids":[18]}]}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.themoviedb.org/3/genre/movie/list?api_key=test-key&language=pt-BR"))
                .andRespond(withSuccess("{\"genres\":[{\"id\":18,\"name\":\"Drama\"}]}", MediaType.APPLICATION_JSON));
        var pesquisa = client.pesquisar("Clube da Luta", 2);
        assertThat(pesquisa.totalResultados()).isEqualTo(42);
        assertThat(pesquisa.resultados().getFirst().generos()).containsExactly("Drama");
    }
    @Test void filmeAusenteRetorna404() {
        server.expect(anything()).andRespond(withResourceNotFound());
        assertThatThrownBy(() -> client.detalhes(1L)).isInstanceOfSatisfying(ApiException.class,
                ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }
    @Test void errosExternosRetornam502SemExporCredenciais() {
        server.expect(anything()).andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("segredo"));
        assertThatThrownBy(() -> client.detalhes(1L)).isInstanceOfSatisfying(ApiException.class, ex -> {
            assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
            assertThat(ex.getMessage()).doesNotContain("segredo", "test-key");
        });
    }
    @Test void respostaInvalidaRetorna502() {
        server.expect(anything()).andRespond(withSuccess("{\"id\":1}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.detalhes(1L)).isInstanceOfSatisfying(ApiException.class,
                ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY));
    }
    @Test void chaveAusenteNaoChamaApi() {
        client = new TmdbClient(RestClient.create(), "");
        assertThatThrownBy(() -> client.detalhes(1L)).isInstanceOfSatisfying(ApiException.class,
                ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY));
    }
    @Test void falhaDeRedeRetorna502() {
        server.expect(anything()).andRespond(withException(new java.net.SocketTimeoutException("timeout")));
        assertThatThrownBy(() -> client.detalhes(1L)).isInstanceOfSatisfying(ApiException.class,
                ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY));
    }
    @Test void pesquisaVaziaNaoConsultaGenerosEProtegeParametros() {
        server.expect(queryParam("query", "A%26page%3D500"))
                .andExpect(queryParam("page", "1"))
                .andRespond(withSuccess("{\"page\":1,\"total_pages\":0,\"total_results\":0,\"results\":[]}", MediaType.APPLICATION_JSON));
        assertThat(client.pesquisar("A&page=500", 1).resultados()).isEmpty();
    }
}
