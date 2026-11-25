package Arthur.OurMovies.dto.response;

import java.util.List;

public record PesquisaFilmesResponse(int pagina,
                                     int totalPaginas,
                                     int totalResultados,
                                     List<FilmeResponse> resultados) {
}
