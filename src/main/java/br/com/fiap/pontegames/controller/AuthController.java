package br.com.fiap.pontegames.controller;

import br.com.fiap.pontegames.dto.LoginRequest;
import br.com.fiap.pontegames.dto.LoginResponse;
import br.com.fiap.pontegames.dto.RegistroRequest;
import br.com.fiap.pontegames.dto.UsuarioResponse;
import br.com.fiap.pontegames.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação", description = "Cadastro público de cidadão e login que devolve o JWT.")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/registrar")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Cadastra um cidadão. O campo tipo, se vier, é ignorado.")
	public UsuarioResponse registrar(@Valid @RequestBody RegistroRequest request) {
		return authService.registrar(request);
	}

	@PostMapping("/login")
	@Operation(summary = "Entra com e-mail e senha e devolve o token Bearer.")
	public LoginResponse login(@Valid @RequestBody LoginRequest request) {
		return authService.login(request);
	}

}
