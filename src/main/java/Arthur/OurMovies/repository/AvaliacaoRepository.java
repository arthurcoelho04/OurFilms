package Arthur.OurMovies.repository;

import Arthur.OurMovies.entity.Avaliacao;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {
    boolean existsByUsuarioIdAndFilmeId(Long usuarioId, Long filmeId);
    boolean existsByFilmeId(Long filmeId);
    List<Avaliacao> findByFilmeIdOrderById(Long filmeId);
    void deleteByFilmeId(Long filmeId);
}
