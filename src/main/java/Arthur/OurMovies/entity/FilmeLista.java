package Arthur.OurMovies.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "filme_lista", uniqueConstraints = @UniqueConstraint(name = "uk_lista_filme", columnNames = "filme_id"))
@Getter
@Setter
@NoArgsConstructor
public class FilmeLista {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "filme_id", nullable = false)
    private Filme filme;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusFilme status;
    @Column(nullable = false)
    private boolean favorito;
    private LocalDate dataAssistido;
}
