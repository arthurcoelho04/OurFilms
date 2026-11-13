package Arthur.OurMovies.dto;

import Arthur.OurMovies.entity.Avaliacao;

import java.math.BigDecimal;

public record AvaliacaoResponse(Long id, UsuarioResponse usuario, Long filmeId, BigDecimal nota, String comentario) {
    public static AvaliacaoResponse from(Avaliacao avaliacao) {
        return new AvaliacaoResponse(avaliacao.getId(), UsuarioResponse.from(avaliacao.getUsuario()),
                avaliacao.getFilme().getId(), avaliacao.getNota(), avaliacao.getComentario());
    }
}
