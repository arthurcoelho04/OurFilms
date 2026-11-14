package Arthur.OurMovies;

import Arthur.OurMovies.repository.UsuarioRepository;
import Arthur.OurMovies.service.FotoStorageService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UsuarioIntegrationTests {
    static final Path FOTOS = temporario();
    @DynamicPropertySource
    static void configurar(DynamicPropertyRegistry registry) {
        registry.add("ourmovies.fotos-diretorio", () -> FOTOS.toString());
    }
    private static Path temporario() {
        try { return Files.createTempDirectory("ourmovies-fotos-"); }
        catch (IOException ex) { throw new UncheckedIOException(ex); }
    }
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired TransactionTemplate transacao;
    private String nomeOriginal;

    @BeforeEach
    void preparar() { nomeOriginal = usuarios.findById(1L).orElseThrow().getNome(); }

    @AfterEach
    void limpar() throws IOException {
        transacao.executeWithoutResult(status -> {
            var usuario = usuarios.findById(1L).orElseThrow();
            usuario.setNome(nomeOriginal);
            usuario.setFotoPerfil(null);
        });
        try (var arquivos = Files.list(FOTOS)) {
            for (var arquivo : arquivos.toList()) Files.delete(arquivo);
        }
    }
    @AfterAll
    static void limparDiretorio() throws IOException { Files.delete(FOTOS); }

    private byte[] imagem(String formato) throws IOException {
        var output = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), formato, output));
        return output.toByteArray();
    }
    private MockMultipartFile foto(String nome, String tipo, byte[] bytes) {
        return new MockMultipartFile("foto", nome, tipo, bytes);
    }
    private String referencia() { return usuarios.findById(1L).orElseThrow().getFotoPerfil(); }

    @Test
    void alteraNomePreservandoIdsEOutroUsuario() throws Exception {
        var outro = usuarios.findById(2L).orElseThrow().getNome();
        mvc.perform(patch("/api/usuarios/1").contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"  Novo nome  \"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nome").value("Novo nome"))
                .andExpect(jsonPath("$.fotoUrl").value("/api/usuarios/1/foto"));
        assertEquals("Novo nome", usuarios.findById(1L).orElseThrow().getNome());
        assertEquals(outro, usuarios.findById(2L).orElseThrow().getNome());
        assertEquals(List.of(1L, 2L), usuarios.findAll(org.springframework.data.domain.Sort.by("id")).stream().map(u -> u.getId()).toList());
    }

    @Test
    void validaNome() throws Exception {
        for (String corpo : List.of("{}", "{\"nome\":null}", "{\"nome\":\"   \"}", "{\"nome\":\"" + "a".repeat(101) + "\"}")) {
            mvc.perform(patch("/api/usuarios/1").contentType(MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isBadRequest());
        }
        assertEquals(nomeOriginal, usuarios.findById(1L).orElseThrow().getNome());
    }

    @Test
    void enviaConsultaSubstituiERemoveComLimpezaFisica() throws Exception {
        mvc.perform(get("/api/usuarios/1/foto")).andExpect(status().isOk()).andExpect(content().contentType("image/svg+xml"));
        byte[] png = imagem("png");
        mvc.perform(multipart("/api/usuarios/1/foto").file(foto("../../fora.png", "image/png", png)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1));
        String primeira = referencia();
        assertTrue(primeira.matches("[0-9a-f-]{36}\\.png"));
        assertArrayEquals(png, Files.readAllBytes(FOTOS.resolve(primeira)));
        mvc.perform(get("/api/usuarios/1/foto")).andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(png)).andExpect(header().string("X-Content-Type-Options", "nosniff"));
        byte[] jpg = imagem("jpg");
        mvc.perform(multipart("/api/usuarios/1/foto").file(foto("C:\\fakepath\\foto.jpg", "image/jpeg", jpg))).andExpect(status().isOk());
        assertFalse(Files.exists(FOTOS.resolve(primeira)));
        String segunda = referencia();
        mvc.perform(get("/api/usuarios/1/foto")).andExpect(content().contentType("image/jpeg")).andExpect(content().bytes(jpg));
        mvc.perform(delete("/api/usuarios/1/foto")).andExpect(status().isNoContent());
        assertNull(referencia());
        assertFalse(Files.exists(FOTOS.resolve(segunda)));
        mvc.perform(delete("/api/usuarios/1/foto")).andExpect(status().isNoContent());
        mvc.perform(get("/api/usuarios/1/foto")).andExpect(content().contentType("image/svg+xml"));
    }

    @Test
    void aceitaWebpERecusaContainerTruncado() throws Exception {
        byte[] webp = HexFormat.of().parseHex("524946461a000000574542505650384c0d0000002f00000000071011118888fe0700");
        mvc.perform(multipart("/api/usuarios/1/foto").file(foto("foto.webp", "image/webp", webp))).andExpect(status().isOk());
        mvc.perform(get("/api/usuarios/1/foto")).andExpect(content().contentType("image/webp")).andExpect(content().bytes(webp));
        mvc.perform(multipart("/api/usuarios/1/foto").file(foto("foto.webp", "image/webp", Arrays.copyOf(webp, 20)))).andExpect(status().isBadRequest());
    }

    @Test
    void rejeitaArquivosInvalidosSemSubstituirFotoAtual() throws Exception {
        byte[] png = imagem("png");
        mvc.perform(multipart("/api/usuarios/1/foto").file(foto("foto.png", "image/png", png))).andExpect(status().isOk());
        String anterior = referencia();
        for (var arquivo : List.of(foto("x.svg", "image/svg+xml", "<svg/>".getBytes()),
                foto("x.png", "image/png", "texto".getBytes()), foto("x.png", "image/png", new byte[0]),
                foto("x.jpg", "image/jpeg", png), foto("x.png", null, png),
                foto("x.png", "image/png", Arrays.copyOf(png, 12)))) {
            mvc.perform(multipart("/api/usuarios/1/foto").file(arquivo)).andExpect(status().isBadRequest());
        }
        mvc.perform(multipart("/api/usuarios/1/foto").file(foto("x.png", "image/png", new byte[FotoStorageService.MAX_BYTES + 1])))
                .andExpect(status().isPayloadTooLarge());
        assertEquals(anterior, referencia());
        assertTrue(Files.exists(FOTOS.resolve(anterior)));
        try (var arquivos = Files.list(FOTOS)) { assertEquals(1, arquivos.count()); }
        mvc.perform(multipart("/api/usuarios/1/foto")).andExpect(status().isBadRequest());
    }

    @Test
    void aceitaWebpAnimadoERejeitaQuadroSemDados() throws Exception {
        byte[] simples = HexFormat.of().parseHex("524946461a000000574542505650384c0d0000002f00000000071011118888fe0700");
        var output = new ByteArrayOutputStream();
        output.write(HexFormat.of().parseHex("524946465200000057454250"));
        output.write(HexFormat.of().parseHex("565038580a00000002000000000000000000"));
        output.write(HexFormat.of().parseHex("414e494d06000000000000000000"));
        output.write(HexFormat.of().parseHex("414e4d4626000000"));
        output.write(new byte[16]);
        output.write(simples, 12, simples.length - 12);
        byte[] animado = output.toByteArray();
        mvc.perform(multipart("/api/usuarios/1/foto").file(foto("foto.webp", "image/webp", animado))).andExpect(status().isOk());
        mvc.perform(get("/api/usuarios/1/foto")).andExpect(content().bytes(animado));
        animado[68] = 0; // Corrompe o FourCC do bitstream do quadro.
        mvc.perform(multipart("/api/usuarios/1/foto").file(foto("foto.webp", "image/webp", animado))).andExpect(status().isBadRequest());
    }

    @Test
    void rollbackDaRemocaoPreservaArquivoEReferencia() throws Exception {
        mvc.perform(multipart("/api/usuarios/1/foto").file(foto("x.png", "image/png", imagem("png")))).andExpect(status().isOk());
        String anterior = referencia();
        transacao.executeWithoutResult(status -> {
            try { mvc.perform(delete("/api/usuarios/1/foto")).andExpect(status().isNoContent()); }
            catch (Exception ex) { throw new RuntimeException(ex); }
            status.setRollbackOnly();
        });
        assertEquals(anterior, referencia());
        assertTrue(Files.exists(FOTOS.resolve(anterior)));
    }

    @Test
    void recursosAusentesERetornoPadraoParaArquivoPerdido() throws Exception {
        mvc.perform(patch("/api/usuarios/999").contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Nome\"}")).andExpect(status().isNotFound());
        mvc.perform(get("/api/usuarios/999/foto")).andExpect(status().isNotFound());
        mvc.perform(delete("/api/usuarios/999/foto")).andExpect(status().isNotFound());
        mvc.perform(multipart("/api/usuarios/999/foto").file(foto("x.png", "image/png", imagem("png")))).andExpect(status().isNotFound());
        transacao.executeWithoutResult(status -> usuarios.findById(1L).orElseThrow().setFotoPerfil(UUID.randomUUID() + ".png"));
        mvc.perform(get("/api/usuarios/1/foto")).andExpect(status().isOk()).andExpect(content().contentType("image/svg+xml"));
    }

    @Test
    void rollbackRemoveNovaFotoEPreservaAnterior() throws Exception {
        mvc.perform(multipart("/api/usuarios/1/foto").file(foto("x.png", "image/png", imagem("png")))).andExpect(status().isOk());
        String anterior = referencia();
        transacao.executeWithoutResult(status -> {
            try {
                mvc.perform(multipart("/api/usuarios/1/foto").file(foto("x.jpg", "image/jpeg", imagem("jpg")))).andExpect(status().isOk());
            } catch (Exception ex) { throw new RuntimeException(ex); }
            status.setRollbackOnly();
        });
        assertEquals(anterior, referencia());
        assertTrue(Files.exists(FOTOS.resolve(anterior)));
        try (var arquivos = Files.list(FOTOS)) { assertEquals(1, arquivos.count()); }
    }
}
