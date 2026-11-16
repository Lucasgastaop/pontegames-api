package br.com.fiap.pontegames.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record TipoResiduoRequest(
		@NotBlank(message = "Nome é obrigatório.")
		@Size(max = 60, message = "Nome deve ter no máximo 60 caracteres.")
		String nome,

		@NotBlank(message = "Instrução é obrigatória.")
		@Size(max = 255, message = "Instrução deve ter no máximo 255 caracteres.")
		String instrucao,

		@NotNull(message = "XP por kg é obrigatório.")
		@Positive(message = "XP por kg deve ser maior que zero.")
		@Digits(integer = 4, fraction = 2, message = "XP por kg deve ter no máximo 4 dígitos inteiros e 2 decimais.")
		BigDecimal xpPorKg
) {
}
