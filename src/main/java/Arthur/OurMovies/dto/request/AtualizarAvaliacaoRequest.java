package Arthur.OurMovies.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record AtualizarAvaliacaoRequest(
        @NotNull @DecimalMin("0.0") @DecimalMax("10.0") @Digits(integer = 2, fraction = 1) BigDecimal nota,
        @Size(max = 2000) String comentario) {

    //@Digits - permite até 2 dígitos inteiros e 1 decimal.
}
