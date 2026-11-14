package Arthur.OurMovies.client;

import Arthur.OurMovies.dto.FilmeResponse;
import Arthur.OurMovies.dto.PesquisaFilmesResponse;
import Arthur.OurMovies.exception.ApiException;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.function.Supplier;

@Component
public class TmdbClient {
    private final RestClient client;
    private final String apiKey;

    public TmdbClient(RestClient tmdbRestClient, @Value("${tmdb.api-key}") String apiKey) {
        this.client = tmdbRestClient;
        this.apiKey = apiKey;
    }

    public record Genero(Long id, String name) {
    }

    public record FilmeTmdb(Long id, String title, String overview, @JsonProperty("poster_path") String posterPath,
                            @JsonProperty("release_date") String releaseDate, List<Genero> genres,
                            @JsonProperty("genre_ids") List<Long> genreIds) {
    }

    public record PesquisaTmdb(Integer page, @JsonProperty("total_pages") Integer totalPages,
                               @JsonProperty("total_results") Integer totalResults, List<FilmeTmdb> results) {
    }

    public record GenerosTmdb(List<Genero> genres) {
    }

    public PesquisaFilmesResponse pesquisar(String nome, int pagina) {
        return executar(() -> {
            var pesquisa = client.get().uri(uri -> uri.path("/search/movie")
                    .queryParam("api_key", apiKey).queryParam("language", "pt-BR")
                    .queryParam("query", "{nome}").queryParam("page", pagina).queryParam("include_adult", false)
                    .build(nome.trim())).retrieve().body(PesquisaTmdb.class);
            if (pesquisa == null || pesquisa.results() == null || pesquisa.page() == null
                    || pesquisa.totalPages() == null || pesquisa.totalResults() == null) throw ApiException.tmdb();
            if (pesquisa.results().isEmpty())
                return new PesquisaFilmesResponse(pesquisa.page(), pesquisa.totalPages(), pesquisa.totalResults(), List.of());
            var generos = client.get().uri(uri -> uri.path("/genre/movie/list")
                            .queryParam("api_key", apiKey).queryParam("language", "pt-BR").build())
                    .retrieve().body(GenerosTmdb.class);
            if (generos == null || generos.genres() == null) throw ApiException.tmdb();
            return new PesquisaFilmesResponse(pesquisa.page(), pesquisa.totalPages(), pesquisa.totalResults(),
                    pesquisa.results().stream().map(f -> mapear(f, generos.genres().stream()
                            .filter(g -> f.genreIds() != null && f.genreIds().contains(g.id()))
                            .map(Genero::name).toList())).toList());
        }, false);
    }

    public FilmeResponse detalhes(Long tmdbId) {
        return executar(() -> {
            var filme = client.get().uri(uri -> uri.path("/movie/{id}")
                            .queryParam("api_key", apiKey).queryParam("language", "pt-BR").build(tmdbId))
                    .retrieve().body(FilmeTmdb.class);
            if (filme == null || filme.genres() == null) throw ApiException.tmdb();
            return mapear(filme, filme.genres().stream().map(Genero::name).toList());
        }, true);
    }

    private FilmeResponse mapear(FilmeTmdb filme, List<String> generos) {
        if (filme.id() == null || filme.title() == null || filme.title().isBlank()) throw ApiException.tmdb();
        LocalDate data = filme.releaseDate() == null || filme.releaseDate().isBlank() ? null : LocalDate.parse(filme.releaseDate());
        return new FilmeResponse(null, filme.id(), filme.title(), filme.overview(), filme.posterPath(), data, generos);
    }

    private <T> T executar(Supplier<T> chamada, boolean detalhes) {
        if (apiKey == null || apiKey.isBlank()) throw ApiException.tmdb();
        try {
            return chamada.get();
        } catch (RestClientResponseException ex) {
            if (detalhes && ex.getStatusCode().value() == 404)
                throw ApiException.notFound("Filme não encontrado na TMDB.");
            throw ApiException.tmdb();
        } catch (RestClientException | DateTimeParseException ex) {
            throw ApiException.tmdb();
        }
    }
}
