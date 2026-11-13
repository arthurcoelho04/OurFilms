package Arthur.OurMovies.controller;

import Arthur.OurMovies.dto.*;
import Arthur.OurMovies.service.AvaliacaoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/avaliacoes")
@RequiredArgsConstructor
public class AvaliacaoController {
    private final AvaliacaoService service;

    @PostMapping
    public ResponseEntity<AvaliacaoResponse> criar(@RequestBody @Valid AvaliacaoRequest request) {
        var response = service.criar(request);
        return ResponseEntity.created(URI.create("/api/avaliacoes/" + response.id())).body(response);
    }

    @GetMapping("/filme/{filmeId}")
    public AvaliacoesFilmeResponse listar(@PathVariable @Positive Long filmeId) {
        return service.listar(filmeId);
    }

    @PutMapping("/{id}")
    public AvaliacaoResponse atualizar(@PathVariable @Positive Long id, @RequestBody @Valid AtualizarAvaliacaoRequest request) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable @Positive Long id) {
        service.remover(id);
        return ResponseEntity.noContent().build();
    }
}
