package Arthur.OurMovies.dto;

import Arthur.OurMovies.entity.Filme;

import java.time.LocalDate;
import java.util.List;

public record FilmeResponse(Long id, Long tmdbId, String titulo, String sinopse, String posterPath,
                            LocalDate dataLancamento, List<String> generos) {
    public static FilmeResponse from(Filme filme) {
        return new FilmeResponse(filme.getId(), filme.getTmdbId(), filme.getTitulo(), filme.getSinopse(),
                filme.getPosterPath(), filme.getDataLancamento(), List.copyOf(filme.getGeneros()));
    }
}
