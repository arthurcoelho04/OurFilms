package Arthur.OurMovies.service;

import Arthur.OurMovies.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Logger;

@Service
public class FotoStorageService {
    public static final int MAX_BYTES = 5 * 1024 * 1024;
    private static final Logger LOG = Logger.getLogger(FotoStorageService.class.getName());
    private final Path diretorio;

    public FotoStorageService(@Value("${ourmovies.fotos-diretorio:./data/fotos}") String diretorio) {
        this.diretorio = Path.of(diretorio).toAbsolutePath().normalize();
        if (this.diretorio.startsWith(Path.of("src").toAbsolutePath().normalize())) {
            throw new IllegalArgumentException("O diretorio de fotos deve ficar fora de src.");
        }
    }

    public String salvar(MultipartFile arquivo) {
        if (arquivo.isEmpty()) throw ApiException.badRequest("Envie uma imagem nao vazia.");
        if (arquivo.getSize() > MAX_BYTES) throw tamanho();
        try (var entrada = arquivo.getInputStream()) {
            byte[] bytes = entrada.readNBytes(MAX_BYTES + 1);
            if (bytes.length > MAX_BYTES) throw tamanho();
            String extensao = validar(bytes, arquivo.getContentType());
            Files.createDirectories(diretorio);
            String referencia = UUID.randomUUID() + "." + extensao;
            try {
                Files.write(caminho(referencia), bytes, StandardOpenOption.CREATE_NEW);
            } catch (IOException ex) {
                remover(referencia);
                throw ex;
            }
            return referencia;
        } catch (IOException ex) {
            throw falha();
        }
    }

    private String validar(byte[] bytes, String tipo) {
        String extensao;
        if (bytes.length >= 12 && ascii(bytes, 0).equals("RIFF") && ascii(bytes, 8).equals("WEBP")) {
            validarWebp(bytes);
            extensao = "webp";
        } else {
            try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                var leitores = ImageIO.getImageReaders(input);
                if (!leitores.hasNext()) throw invalida();
                var leitor = leitores.next();
                try {
                    leitor.setInput(input);
                    extensao = switch (leitor.getFormatName().toLowerCase(Locale.ROOT)) {
                        case "jpeg", "jpg" -> "jpg";
                        case "png" -> "png";
                        default -> throw invalida();
                    };
                    dimensoes(leitor.getWidth(0), leitor.getHeight(0));
                    if (leitor.read(0) == null) throw invalida();
                } finally {
                    leitor.dispose();
                }
            } catch (IOException | IllegalArgumentException ex) {
                throw invalida();
            }
        }
        String esperado = extensao.equals("jpg") ? "image/jpeg" : "image/" + extensao;
        if (!esperado.equalsIgnoreCase(tipo)) throw invalida();
        return extensao;
    }

    private String ascii(byte[] b, int pos) {
        return new String(b, pos, 4, java.nio.charset.StandardCharsets.US_ASCII);
    }

    // O JDK nao inclui decoder WebP: verifica container RIFF e cabecalhos VP8/VP8L.
    private void validarWebp(byte[] b) {
        var buffer = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN);
        if (Integer.toUnsignedLong(buffer.getInt(4)) + 8 != b.length) throw invalida();
        validarChunks(b, 12, b.length, false);
    }

    private int uint24(byte[] b, int pos) {
        return (b[pos] & 255) | ((b[pos + 1] & 255) << 8) | ((b[pos + 2] & 255) << 16);
    }

    private void validarChunks(byte[] b, int pos, int limite, boolean quadro) {
        var buffer = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN);
        boolean imagem = false;
        boolean animacao = false;
        boolean controleAnimacao = false;
        boolean quadros = false;
        int larguraCanvas = 0;
        int alturaCanvas = 0;
        while (pos + 8 <= limite) {
            String chunk = ascii(b, pos);
            long tamanho = Integer.toUnsignedLong(buffer.getInt(pos + 4));
            long fim = pos + 8L + tamanho;
            if (fim + (tamanho & 1) > limite || ((tamanho & 1) != 0 && b[(int) fim] != 0)) throw invalida();
            int inicio = pos + 8;
            if (chunk.equals("VP8 ")) {
                if (animacao || imagem || tamanho <= 10 || (b[inicio] & 1) != 0 || b[inicio + 3] != (byte) 0x9d
                        || b[inicio + 4] != 1 || b[inicio + 5] != 0x2a) throw invalida();
                dimensoes(buffer.getShort(inicio + 6) & 0x3fff, buffer.getShort(inicio + 8) & 0x3fff);
                imagem = true;
            } else if (chunk.equals("VP8L")) {
                if (animacao || imagem || tamanho <= 5 || b[inicio] != 0x2f) throw invalida();
                int bits = buffer.getInt(inicio + 1);
                if ((bits >>> 29) != 0) throw invalida();
                dimensoes((bits & 0x3fff) + 1, ((bits >>> 14) & 0x3fff) + 1);
                imagem = true;
            } else if (chunk.equals("VP8X")) {
                if (quadro || pos != 12 || tamanho != 10 || (b[inicio] & 0xc1) != 0
                        || b[inicio + 1] != 0 || b[inicio + 2] != 0 || b[inicio + 3] != 0) throw invalida();
                larguraCanvas = uint24(b, inicio + 4) + 1;
                alturaCanvas = uint24(b, inicio + 7) + 1;
                dimensoes(larguraCanvas, alturaCanvas);
                animacao = (b[inicio] & 2) != 0;
            } else if (chunk.equals("ANIM")) {
                if (quadro || !animacao || controleAnimacao || tamanho != 6) throw invalida();
                controleAnimacao = true;
            } else if (chunk.equals("ANMF")) {
                if (quadro || !controleAnimacao || tamanho < 16 || (b[inicio + 15] & 0xfc) != 0) throw invalida();
                int largura = uint24(b, inicio + 6) + 1;
                int altura = uint24(b, inicio + 9) + 1;
                dimensoes(largura, altura);
                if (2L * uint24(b, inicio) + largura > larguraCanvas
                        || 2L * uint24(b, inicio + 3) + altura > alturaCanvas) throw invalida();
                validarChunks(b, inicio + 16, (int) fim, true);
                quadros = true;
            }
            pos = (int) (fim + (tamanho & 1));
        }
        if (pos != limite || (!imagem && !quadros)) throw invalida();
    }

    private void dimensoes(int largura, int altura) {
        if (largura <= 0 || altura <= 0 || (long) largura * altura > 25_000_000) throw invalida();
    }

    public byte[] ler(String referencia) {
        try {
            Path path = caminho(referencia);
            if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) return null;
            return Files.readAllBytes(path);
        } catch (IOException ex) {
            throw falha();
        }
    }

    public void remover(String referencia) {
        if (referencia == null) return;
        try {
            Files.deleteIfExists(caminho(referencia));
        } catch (IOException ex) {
            LOG.warning("Nao foi possivel remover uma foto antiga do armazenamento.");
        }
    }

    private Path caminho(String referencia) {
        if (referencia == null || !referencia.matches("[0-9a-f-]{36}\\.(jpg|png|webp)")) throw falha();
        return diretorio.resolve(referencia);
    }

    private ApiException invalida() {
        return ApiException.badRequest("Imagem invalida. Use JPG, PNG ou WebP, MIME correspondente e ate 25 megapixels.");
    }

    private ApiException tamanho() {
        return new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "A foto deve ter no maximo 5 MB.");
    }

    private ApiException falha() {
        return new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Nao foi possivel acessar o armazenamento de fotos.");
    }
}
