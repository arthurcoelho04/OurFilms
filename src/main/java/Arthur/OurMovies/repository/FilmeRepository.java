package Arthur.OurMovies.repository;
import Arthur.OurMovies.entity.Filme;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface FilmeRepository extends JpaRepository<Filme, Long> {
    Optional<Filme> findByTmdbId(Long tmdbId);
}
