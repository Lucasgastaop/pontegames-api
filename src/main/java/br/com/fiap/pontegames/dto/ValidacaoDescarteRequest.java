package br.com.fiap.pontegames.dto;

import br.com.fiap.pontegames.model.StatusValidacao;
import jakarta.validation.constraints.NotNull;

public record ValidacaoDescarteRequest(
		@NotNull(message = "Status da validação é obrigatório.")
		StatusValidacao status
) {
}
