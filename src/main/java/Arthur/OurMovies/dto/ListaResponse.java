package Arthur.OurMovies.dto;

import Arthur.OurMovies.entity.*;

import java.time.LocalDate;

public record ListaResponse(Long id, FilmeResponse filme, StatusFilme status, boolean favorito,
                            LocalDate dataAssistido, Double mediaAvaliacoes, long totalAvaliacoes) {
    public static ListaResponse from(FilmeLista item, Double media, long total) {
        return new ListaResponse(item.getId(), FilmeResponse.from(item.getFilme()), item.getStatus(),
                item.isFavorito(), item.getDataAssistido(), media, total);
    }
}
