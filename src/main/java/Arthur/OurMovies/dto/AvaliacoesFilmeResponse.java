package Arthur.OurMovies.dto;

import java.util.List;

public record AvaliacoesFilmeResponse(Long filmeId, Double media, long total, List<AvaliacaoResponse> avaliacoes) {
}
