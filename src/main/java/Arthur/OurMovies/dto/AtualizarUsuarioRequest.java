package Arthur.OurMovies.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AtualizarUsuarioRequest(@NotBlank @Size(max = 100) String nome) {}
