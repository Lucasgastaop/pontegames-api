package br.com.fiap.pontegames.messaging;

import br.com.fiap.pontegames.model.StatusValidacao;

import java.math.BigDecimal;

public record ResultadoDescarte(
		Long descarteId,
		Long usuarioId,
		String nomeUsuario,
		String nomeResiduo,
		BigDecimal xpGanho,
		BigDecimal xpTotal,
		Integer nivel,
		StatusValidacao status
) {
}
