package Arthur.OurMovies.controller;

import Arthur.OurMovies.dto.AtualizarUsuarioRequest;
import Arthur.OurMovies.dto.UsuarioResponse;
import Arthur.OurMovies.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {
    private final UsuarioService service;

    @GetMapping
    public List<UsuarioResponse> listar() { return service.listar(); }

    @PatchMapping("/{id}")
    public UsuarioResponse atualizar(@PathVariable Long id, @Valid @RequestBody AtualizarUsuarioRequest request) {
        return service.atualizar(id, request);
    }

    @PostMapping(value = "/{id}/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UsuarioResponse enviarFoto(@PathVariable Long id, @RequestPart("foto") MultipartFile foto) {
        return service.enviarFoto(id, foto);
    }

    @GetMapping("/{id}/foto")
    public ResponseEntity<byte[]> consultarFoto(@PathVariable Long id) {
        var foto = service.consultarFoto(id);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(foto.tipo()))
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.noStore()).body(foto.bytes());
    }

    @DeleteMapping("/{id}/foto")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removerFoto(@PathVariable Long id) { service.removerFoto(id); }
}
