package br.com.fiap.pontegames.dto;

import java.math.BigDecimal;

public record DescarteRequest(
		Long usuarioId,
		Long pontoId,
		Long tipoResiduoId,
		BigDecimal pesoKg,
		String fotoUrl
) {
}
