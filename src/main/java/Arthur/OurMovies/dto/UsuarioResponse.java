package Arthur.OurMovies.dto;

import Arthur.OurMovies.entity.Usuario;

public record UsuarioResponse(Long id, String nome) {
    public static UsuarioResponse from(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNome());
    }
}
