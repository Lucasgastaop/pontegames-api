package br.com.fiap.pontegames.dto;

import java.math.BigDecimal;

public record PontoColetaRequest(
		String nome,
		String endereco,
		BigDecimal latitude,
		BigDecimal longitude,
		Boolean ativo
) {
}
