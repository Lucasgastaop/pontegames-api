package br.com.fiap.pontegames.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PontoColetaRequest(
		@NotBlank(message = "Nome é obrigatório.")
		@Size(max = 100, message = "Nome deve ter no máximo 100 caracteres.")
		String nome,

		@NotBlank(message = "Endereço é obrigatório.")
		@Size(max = 200, message = "Endereço deve ter no máximo 200 caracteres.")
		String endereco,

		@NotNull(message = "Latitude é obrigatória.")
		@DecimalMin(value = "-90.0", message = "Latitude deve ser entre -90 e 90.")
		@DecimalMax(value = "90.0", message = "Latitude deve ser entre -90 e 90.")
		@Digits(integer = 2, fraction = 8, message = "Latitude deve ter no máximo 2 dígitos inteiros e 8 decimais.")
		BigDecimal latitude,

		@NotNull(message = "Longitude é obrigatória.")
		@DecimalMin(value = "-180.0", message = "Longitude deve ser entre -180 e 180.")
		@DecimalMax(value = "180.0", message = "Longitude deve ser entre -180 e 180.")
		@Digits(integer = 3, fraction = 8, message = "Longitude deve ter no máximo 3 dígitos inteiros e 8 decimais.")
		BigDecimal longitude,

		Boolean ativo
) {
}
