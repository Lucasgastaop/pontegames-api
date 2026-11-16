package br.com.fiap.pontegames.dto;

import br.com.fiap.pontegames.model.TipoUsuario;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UsuarioResponse(
		Long id,
		String nome,
		String email,
		String cpf,
		Integer nivel,
		BigDecimal xpTotal,
		TipoUsuario tipo,
		LocalDateTime dataCadastro
) {
}
