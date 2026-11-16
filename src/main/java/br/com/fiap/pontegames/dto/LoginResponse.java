package br.com.fiap.pontegames.dto;

import br.com.fiap.pontegames.model.TipoUsuario;

public record LoginResponse(String token, String tipoToken, Long id, String nome, TipoUsuario tipo) {
}
