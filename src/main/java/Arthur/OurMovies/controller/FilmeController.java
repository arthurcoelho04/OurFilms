package Arthur.OurMovies.controller;

import Arthur.OurMovies.dto.FilmeResponse;
import Arthur.OurMovies.dto.PesquisaFilmesResponse;
import Arthur.OurMovies.service.FilmeService;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/filmes")
@RequiredArgsConstructor
public class FilmeController {
    private final FilmeService service;

    @GetMapping("/pesquisar")
    public PesquisaFilmesResponse pesquisar(@RequestParam @NotBlank @Size(max = 200) String nome,
                                            @RequestParam(defaultValue = "1") @Min(1) @Max(500) int pagina) {
        return service.pesquisar(nome, pagina);
    }

    @GetMapping("/{tmdbId}/detalhes")
    public FilmeResponse detalhes(@PathVariable @Positive Long tmdbId) {
        return service.detalhes(tmdbId);
    }
}
