package Arthur.OurMovies.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "filmes", uniqueConstraints = @UniqueConstraint(name = "uk_filme_tmdb", columnNames = "tmdb_id"))
@Getter
@Setter
@NoArgsConstructor
public class Filme {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "tmdb_id", nullable = false)
    private Long tmdbId;
    @Column(nullable = false)
    private String titulo;
    @Column(columnDefinition = "text")
    private String sinopse;
    private String posterPath;
    private LocalDate dataLancamento;
    @ElementCollection
    @CollectionTable(name = "filme_generos", joinColumns = @JoinColumn(name = "filme_id"))
    @Column(name = "genero", nullable = false)
    @OrderColumn(name = "posicao")
    private List<String> generos = new ArrayList<>();
}
