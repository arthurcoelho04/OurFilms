package Arthur.OurMovies.service;

import Arthur.OurMovies.client.TmdbClient;
import Arthur.OurMovies.dto.*;
import Arthur.OurMovies.entity.*;
import Arthur.OurMovies.exception.ApiException;
import Arthur.OurMovies.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListaService {
    private final FilmeListaRepository listaRepository;
    private final FilmeRepository filmeRepository;
    private final AvaliacaoRepository avaliacaoRepository;
    private final TmdbClient tmdbClient;

    public List<ListaResponse> listar() {
        return listaRepository.findAll(Sort.by("id")).stream().map(this::response).toList();
    }

    @Transactional
    public ListaResponse adicionar(Long tmdbId, ListaRequest request) {
        if (listaRepository.existsByFilmeTmdbId(tmdbId))
            throw ApiException.conflict("Filme já está na lista compartilhada.");
        var filme = filmeRepository.findByTmdbId(tmdbId).orElseGet(() -> {
            var dados = tmdbClient.detalhes(tmdbId);
            var novo = new Filme();
            novo.setTmdbId(dados.tmdbId());
            novo.setTitulo(dados.titulo());
            novo.setSinopse(dados.sinopse());
            novo.setPosterPath(dados.posterPath());
            novo.setDataLancamento(dados.dataLancamento());
            novo.setGeneros(new ArrayList<>(dados.generos()));
            return filmeRepository.save(novo);
        });
        var item = new FilmeLista();
        item.setFilme(filme);
        item.setStatus(StatusFilme.QUERO_ASSISTIR);
        aplicar(item, request);
        return response(listaRepository.saveAndFlush(item));
    }

    @Transactional
    public ListaResponse atualizar(Long id, ListaRequest request) {
        var item = listaRepository.buscarParaAtualizar(id).orElseThrow(() -> ApiException.notFound("Item da lista não encontrado."));
        aplicar(item, request);
        return response(item);
    }

    @Transactional
    public void remover(Long id) {
        var item = listaRepository.buscarParaAtualizar(id).orElseThrow(() -> ApiException.notFound("Item da lista não encontrado."));
        avaliacaoRepository.deleteByFilmeId(item.getFilme().getId());
        listaRepository.delete(item);
    }

    private void aplicar(FilmeLista item, ListaRequest request) {
        if (request == null) return;
        var status = request.status() == null ? item.getStatus() : request.status();
        if (status == StatusFilme.QUERO_ASSISTIR) {
            if (request.dataAssistido() != null)
                throw ApiException.badRequest("Data assistido exige status ASSISTIDO.");
            if (item.getId() != null && avaliacaoRepository.existsByFilmeId(item.getFilme().getId()))
                throw ApiException.conflict("Remova as avaliações antes de voltar para QUERO_ASSISTIR.");
            item.setDataAssistido(null);
        } else {
            if (request.dataAssistido() != null) item.setDataAssistido(request.dataAssistido());
            else if (item.getDataAssistido() == null) item.setDataAssistido(LocalDate.now());
        }
        item.setStatus(status);
        if (request.favorito() != null) item.setFavorito(request.favorito());
    }

    private ListaResponse response(FilmeLista item) {
        var avaliacoes = avaliacaoRepository.findByFilmeIdOrderById(item.getFilme().getId());
        Double media = avaliacoes.isEmpty() ? null : avaliacoes.stream().mapToDouble(a -> a.getNota().doubleValue()).average().orElseThrow();
        return ListaResponse.from(item, media, avaliacoes.size());
    }
}
