package br.com.fiap.pontegames.security;

import br.com.fiap.pontegames.model.TipoUsuario;

public record UsuarioAutenticado(Long id, String email, TipoUsuario tipo) {
}
