package Arthur.OurMovies.dto.request;

import Arthur.OurMovies.entity.StatusFilme;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;

/**
 * Campos omitidos no PATCH preservam os valores atuais.
 */
public record ListaRequest(
        StatusFilme status,
        Boolean favorito,
        @PastOrPresent LocalDate dataAssistido) {
}
