package Arthur.OurMovies.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "avaliacoes", uniqueConstraints = @UniqueConstraint(name = "uk_avaliacao_usuario_filme", columnNames = {"usuario_id", "filme_id"}))
@Getter
@Setter
@NoArgsConstructor
public class Avaliacao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "filme_id", nullable = false)
    private Filme filme;
    @Column(nullable = false, precision = 3, scale = 1)
    private BigDecimal nota;
    @Column(length = 2000)
    private String comentario;
}
