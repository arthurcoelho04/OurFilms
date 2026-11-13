package Arthur.OurMovies.config;

import Arthur.OurMovies.entity.Usuario;
import Arthur.OurMovies.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UsuariosInitializer implements ApplicationRunner {

    private final UsuarioRepository repository;
    @Value("${ourmovies.usuario1-nome}")
    private String nome1;
    @Value("${ourmovies.usuario2-nome}")
    private String nome2;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        criar(1L, nome1);
        criar(2L, nome2);
    }

    private void criar(Long id, String nome) {
        if (repository.existsById(id)) return;
        if (nome == null || nome.isBlank() || nome.length() > 100) {
            throw new IllegalStateException("O nome inicial de cada usuário deve ter entre 1 e 100 caracteres.");
        }
        var usuario = new Usuario();
        usuario.setId(id);
        usuario.setNome(nome.trim());
        repository.save(usuario);
    }
}
