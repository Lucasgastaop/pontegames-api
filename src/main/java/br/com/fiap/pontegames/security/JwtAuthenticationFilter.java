package br.com.fiap.pontegames.security;

import br.com.fiap.pontegames.model.Usuario;
import br.com.fiap.pontegames.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtService jwtService;
	private final UsuarioRepository usuarioRepository;

	public JwtAuthenticationFilter(JwtService jwtService, UsuarioRepository usuarioRepository) {
		this.jwtService = jwtService;
		this.usuarioRepository = usuarioRepository;
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain
	) throws ServletException, IOException {
		String cabecalho = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (cabecalho != null && cabecalho.startsWith("Bearer ") && SecurityContextHolder.getContext().getAuthentication() == null) {
			String token = cabecalho.substring(7);
			if (jwtService.valido(token)) {
				autenticar(token);
			}
		}
		filterChain.doFilter(request, response);
	}

	private void autenticar(String token) {
		String id = jwtService.ler(token).getSubject();
		try {
			Long usuarioId = Long.valueOf(id);
			usuarioRepository.findById(usuarioId).ifPresent(this::registrar);
		} catch (NumberFormatException ex) {
			SecurityContextHolder.clearContext();
		}
	}

	private void registrar(Usuario usuario) {
		UsuarioAutenticado atual = new UsuarioAutenticado(usuario.getId(), usuario.getEmail(), usuario.getTipo());
		UsernamePasswordAuthenticationToken autenticacao = new UsernamePasswordAuthenticationToken(
				atual,
				null,
				List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getTipo().name()))
		);
		SecurityContextHolder.getContext().setAuthentication(autenticacao);
	}

}
