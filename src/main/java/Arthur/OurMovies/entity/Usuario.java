package Arthur.OurMovies.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {
    @Id
    private Long id;
    @Column(nullable = false, length = 100)
    private String nome;
}
