package br.com.fiap.pontegames.dto;

import java.math.BigDecimal;

public record PontoColetaResponse(
		Long id,
		String nome,
		String endereco,
		BigDecimal latitude,
		BigDecimal longitude,
		boolean ativo
) {
}
