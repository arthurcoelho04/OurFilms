package Arthur.OurMovies.service;

import Arthur.OurMovies.dto.UsuarioResponse;
import Arthur.OurMovies.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioService {
    private final UsuarioRepository repository;

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return repository.findAll(Sort.by("id")).stream().map(UsuarioResponse::from).toList();
    }
}
