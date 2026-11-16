package br.com.fiap.pontegames.dto;

import br.com.fiap.pontegames.model.StatusValidacao;

import java.time.LocalDateTime;

public record NotificacaoResponse(
		Long id,
		Long descarteId,
		StatusValidacao status,
		String mensagem,
		LocalDateTime dataCriacao
) {
}
