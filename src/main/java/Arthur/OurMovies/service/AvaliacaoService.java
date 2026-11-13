package Arthur.OurMovies.service;

import Arthur.OurMovies.dto.*;
import Arthur.OurMovies.entity.*;
import Arthur.OurMovies.exception.ApiException;
import Arthur.OurMovies.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor @Transactional(readOnly = true)
public class AvaliacaoService {
    private final AvaliacaoRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final FilmeRepository filmeRepository;
    private final FilmeListaRepository listaRepository;

    @Transactional
    public AvaliacaoResponse criar(AvaliacaoRequest request) {
        var usuario = usuarioRepository.findById(request.usuarioId()).orElseThrow(() -> ApiException.notFound("Usuário não encontrado."));
        var item = assistido(request.filmeId());
        if (repository.existsByUsuarioIdAndFilmeId(request.usuarioId(), request.filmeId()))
            throw ApiException.conflict("Usuário já avaliou este filme.");
        var avaliacao = new Avaliacao();
        avaliacao.setUsuario(usuario);
        avaliacao.setFilme(item.getFilme());
        avaliacao.setNota(request.nota());
        avaliacao.setComentario(request.comentario());
        return AvaliacaoResponse.from(repository.saveAndFlush(avaliacao));
    }
    public AvaliacoesFilmeResponse listar(Long filmeId) {
        if (!filmeRepository.existsById(filmeId)) throw ApiException.notFound("Filme não encontrado.");
        var avaliacoes = repository.findByFilmeIdOrderById(filmeId);
        Double media = avaliacoes.isEmpty() ? null : avaliacoes.stream().mapToDouble(a -> a.getNota().doubleValue()).average().orElseThrow();
        return new AvaliacoesFilmeResponse(filmeId, media, avaliacoes.size(), avaliacoes.stream().map(AvaliacaoResponse::from).toList());
    }
    @Transactional
    public AvaliacaoResponse atualizar(Long id, AtualizarAvaliacaoRequest request) {
        var avaliacao = buscar(id);
        assistido(avaliacao.getFilme().getId());
        avaliacao.setNota(request.nota());
        avaliacao.setComentario(request.comentario());
        return AvaliacaoResponse.from(avaliacao);
    }
    @Transactional
    public void remover(Long id) {
        var avaliacao = buscar(id);
        listaRepository.buscarPorFilmeParaAtualizar(avaliacao.getFilme().getId());
        repository.delete(avaliacao);
    }
    private Avaliacao buscar(Long id) { return repository.findById(id).orElseThrow(() -> ApiException.notFound("Avaliação não encontrada.")); }
    private FilmeLista assistido(Long filmeId) {
        var item = listaRepository.buscarPorFilmeParaAtualizar(filmeId)
                .orElseThrow(() -> ApiException.notFound("Filme não encontrado na lista compartilhada."));
        if (item.getStatus() != StatusFilme.ASSISTIDO) throw ApiException.conflict("Somente filmes assistidos podem receber avaliações.");
        return item;
    }
}
