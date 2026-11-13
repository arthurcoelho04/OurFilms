package Arthur.OurMovies.service;

import Arthur.OurMovies.client.TmdbClient;
import Arthur.OurMovies.dto.FilmeResponse;
import Arthur.OurMovies.dto.PesquisaFilmesResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service @RequiredArgsConstructor
public class FilmeService {
    private final TmdbClient client;
    public PesquisaFilmesResponse pesquisar(String nome, int pagina) { return client.pesquisar(nome, pagina); }
    public FilmeResponse detalhes(Long tmdbId) { return client.detalhes(tmdbId); }
}
