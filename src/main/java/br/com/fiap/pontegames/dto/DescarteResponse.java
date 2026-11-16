package br.com.fiap.pontegames.dto;

import br.com.fiap.pontegames.model.StatusValidacao;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DescarteResponse(
		Long id,
		Long usuarioId,
		String nomeUsuario,
		Long pontoId,
		String nomePonto,
		Long tipoResiduoId,
		String nomeResiduo,
		BigDecimal pesoKg,
		BigDecimal xpGanho,
		String fotoUrl,
		StatusValidacao status,
		LocalDateTime dataDescarte
) {
}
