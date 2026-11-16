package br.com.fiap.pontegames.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record DescarteRequest(
		@NotNull(message = "Usuário é obrigatório.")
		@Positive(message = "Usuário inválido.")
		Long usuarioId,

		@NotNull(message = "Ecoponto é obrigatório.")
		@Positive(message = "Ecoponto inválido.")
		Long pontoId,

		@NotNull(message = "Tipo de resíduo é obrigatório.")
		@Positive(message = "Tipo de resíduo inválido.")
		Long tipoResiduoId,

		@NotNull(message = "Peso é obrigatório.")
		@Positive(message = "Peso deve ser maior que zero.")
		@Digits(integer = 4, fraction = 2, message = "Peso deve ter no máximo 4 dígitos inteiros e 2 decimais.")
		BigDecimal pesoKg,

		@Size(max = 500, message = "URL da foto deve ter no máximo 500 caracteres.")
		String fotoUrl
) {
}
