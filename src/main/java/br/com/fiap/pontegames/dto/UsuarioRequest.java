package br.com.fiap.pontegames.dto;

import br.com.fiap.pontegames.model.TipoUsuario;

public record UsuarioRequest(String nome, String email, String cpf, TipoUsuario tipo) {
}
