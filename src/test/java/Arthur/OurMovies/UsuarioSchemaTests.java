package Arthur.OurMovies;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import java.sql.*;
import static org.junit.jupiter.api.Assertions.*;

class UsuarioSchemaTests {
    @Test
    void atualizaEsquemaAnteriorSemApagarUsuariosOuAvaliacoes() throws Exception {
        var datasource = new DriverManagerDataSource("jdbc:h2:mem:migracao;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        var script = new ResourceDatabasePopulator(new ClassPathResource("schema.sql"));
        script.execute(datasource);
        try (var conexao = datasource.getConnection(); var sql = conexao.createStatement()) {
            sql.execute("ALTER TABLE usuarios DROP COLUMN foto_perfil");
            sql.execute("INSERT INTO usuarios(id,nome) VALUES (1,'Arthur'),(2,'Outro nome')");
            sql.execute("INSERT INTO filmes(id,tmdb_id,titulo) VALUES (10,550,'Filme')");
            sql.execute("INSERT INTO avaliacoes(id,usuario_id,filme_id,nota,comentario) VALUES (20,1,10,8.5,'Preservar')");
        }
        script.execute(datasource);
        script.execute(datasource);
        try (var conexao = datasource.getConnection(); var sql = conexao.createStatement()) {
            try (var usuarios = sql.executeQuery("SELECT id,nome,foto_perfil FROM usuarios ORDER BY id")) {
                assertTrue(usuarios.next());
                assertEquals(1, usuarios.getLong("id"));
                assertEquals("Arthur", usuarios.getString("nome"));
                assertNull(usuarios.getString("foto_perfil"));
                assertTrue(usuarios.next());
                assertEquals(2, usuarios.getLong("id"));
                assertEquals("Outro nome", usuarios.getString("nome"));
                assertFalse(usuarios.next());
            }
            try (var avaliacoes = sql.executeQuery("SELECT * FROM avaliacoes")) {
                assertTrue(avaliacoes.next());
                assertEquals(20, avaliacoes.getLong("id"));
                assertEquals(1, avaliacoes.getLong("usuario_id"));
                assertEquals(8.5, avaliacoes.getDouble("nota"));
                assertEquals("Preservar", avaliacoes.getString("comentario"));
                assertFalse(avaliacoes.next());
            }
        }
    }
}
