package br.com.fiap.pontegames.service;

import br.com.fiap.pontegames.dto.LoginRequest;
import br.com.fiap.pontegames.dto.LoginResponse;
import br.com.fiap.pontegames.dto.RegistroRequest;
import br.com.fiap.pontegames.dto.UsuarioResponse;
import br.com.fiap.pontegames.model.Usuario;
import br.com.fiap.pontegames.repository.UsuarioRepository;
import br.com.fiap.pontegames.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

	private final UsuarioRepository usuarioRepository;
	private final UsuarioService usuarioService;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;

	public AuthService(
			UsuarioRepository usuarioRepository,
			UsuarioService usuarioService,
			PasswordEncoder passwordEncoder,
			JwtService jwtService
	) {
		this.usuarioRepository = usuarioRepository;
		this.usuarioService = usuarioService;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
	}

	public UsuarioResponse registrar(RegistroRequest request) {
		return usuarioService.registrarCidadao(request);
	}

	public LoginResponse login(LoginRequest request) {
		Usuario usuario = usuarioRepository.findByEmail(request.email().trim())
				.orElseThrow(this::credenciaisInvalidas);
		if (usuario.getSenha() == null || !passwordEncoder.matches(request.senha(), usuario.getSenha())) {
			throw credenciaisInvalidas();
		}
		return new LoginResponse(jwtService.gerar(usuario), "Bearer", usuario.getId(), usuario.getNome(), usuario.getTipo());
	}

	private ResponseStatusException credenciaisInvalidas() {
		return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos.");
	}

}
