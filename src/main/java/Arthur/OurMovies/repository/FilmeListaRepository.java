package Arthur.OurMovies.repository;

import Arthur.OurMovies.entity.FilmeLista;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FilmeListaRepository extends JpaRepository<FilmeLista, Long> {
    boolean existsByFilmeTmdbId(Long tmdbId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from FilmeLista l where l.id = :id")
    Optional<FilmeLista> buscarParaAtualizar(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from FilmeLista l where l.filme.id = :filmeId")
    Optional<FilmeLista> buscarPorFilmeParaAtualizar(@Param("filmeId") Long filmeId);
}
