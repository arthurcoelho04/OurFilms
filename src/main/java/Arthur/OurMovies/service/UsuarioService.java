package Arthur.OurMovies.service;

import Arthur.OurMovies.dto.AtualizarUsuarioRequest;
import Arthur.OurMovies.dto.UsuarioResponse;
import Arthur.OurMovies.entity.Usuario;
import Arthur.OurMovies.exception.ApiException;
import Arthur.OurMovies.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioService {
    private final UsuarioRepository repository;
    private final FotoStorageService fotos;

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return repository.findAll(Sort.by("id")).stream().map(UsuarioResponse::from).toList();
    }

    @Transactional
    public UsuarioResponse atualizar(Long id, AtualizarUsuarioRequest request) {
        var usuario = buscarParaAtualizar(id);
        usuario.setNome(request.nome().strip());
        return UsuarioResponse.from(usuario);
    }

    @Transactional
    public UsuarioResponse enviarFoto(Long id, MultipartFile arquivo) {
        var usuario = buscarParaAtualizar(id);
        String anterior = usuario.getFotoPerfil();
        String nova = fotos.salvar(arquivo);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                fotos.remover(status == STATUS_COMMITTED ? anterior : nova);
            }
        });
        usuario.setFotoPerfil(nova);
        repository.flush();
        return UsuarioResponse.from(usuario);
    }

    @Transactional
    public void removerFoto(Long id) {
        var usuario = buscarParaAtualizar(id);
        String anterior = usuario.getFotoPerfil();
        usuario.setFotoPerfil(null);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { fotos.remover(anterior); }
        });
    }

    public record Foto(byte[] bytes, String tipo) {}

    @Transactional(readOnly = true)
    public Foto consultarFoto(Long id) {
        var usuario = repository.findById(id).orElseThrow(() -> ApiException.notFound("Usuario nao encontrado."));
        String referencia = usuario.getFotoPerfil();
        byte[] bytes = referencia == null ? null : fotos.ler(referencia);
        if (bytes != null) {
            String extensao = referencia.substring(referencia.lastIndexOf('.') + 1);
            return new Foto(bytes, extensao.equals("jpg") ? "image/jpeg" : "image/" + extensao);
        }
        try (var stream = new ClassPathResource("static/perfil-padrao.svg").getInputStream()) {
            return new Foto(stream.readAllBytes(), "image/svg+xml");
        } catch (IOException ex) { throw new IllegalStateException("Foto padrao indisponivel.", ex); }
    }

    private Usuario buscarParaAtualizar(Long id) {
        return repository.buscarParaAtualizar(id).orElseThrow(() -> ApiException.notFound("Usuario nao encontrado."));
    }
}
