package br.com.fiap.pontegames.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegistroRequest(
		@NotBlank(message = "Nome é obrigatório.")
		@Size(max = 100, message = "Nome deve ter no máximo 100 caracteres.")
		String nome,

		@NotBlank(message = "E-mail é obrigatório.")
		@Email(message = "E-mail inválido.")
		@Size(max = 120, message = "E-mail deve ter no máximo 120 caracteres.")
		String email,

		@NotBlank(message = "CPF é obrigatório.")
		@Pattern(regexp = "\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}", message = "CPF deve estar no formato 000.000.000-00.")
		String cpf,

		@NotBlank(message = "Senha é obrigatória.")
		@Size(min = 6, max = 72, message = "Senha deve ter de 6 a 72 caracteres.")
		String senha
) {
}
