package Arthur.OurMovies.controller;

import Arthur.OurMovies.dto.*;
import Arthur.OurMovies.service.ListaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/lista")
@RequiredArgsConstructor
public class ListaController {
    private final ListaService service;

    @GetMapping
    public List<ListaResponse> listar() {
        return service.listar();
    }

    @PostMapping("/{tmdbId}")
    public ResponseEntity<ListaResponse> adicionar(@PathVariable @Positive Long tmdbId,
                                                   @RequestBody(required = false) @Valid ListaRequest request) {
        var response = service.adicionar(tmdbId, request);
        return ResponseEntity.created(URI.create("/api/lista/" + response.id())).body(response);
    }

    @PatchMapping("/{id}")
    public ListaResponse atualizar(@PathVariable @Positive Long id, @RequestBody @Valid ListaRequest request) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable @Positive Long id) {
        service.remover(id);
        return ResponseEntity.noContent().build();
    }
}
