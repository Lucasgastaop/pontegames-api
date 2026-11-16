package br.com.fiap.pontegames.security;

import br.com.fiap.pontegames.dto.ErroResponse;
import br.com.fiap.pontegames.model.Usuario;
import br.com.fiap.pontegames.repository.UsuarioRepository;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.List;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			JwtAuthenticationFilter jwtAuthenticationFilter,
			ObjectMapper objectMapper
	) throws Exception {
		return http
				.csrf(AbstractHttpConfigurer::disable)
				.cors(cors -> cors.configurationSource(corsConfigurationSource()))
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(ex -> ex
						.authenticationEntryPoint((request, response, authException) -> escrever(
								response, objectMapper, HttpStatus.UNAUTHORIZED, "Autenticação necessária."))
						.accessDeniedHandler((request, response, accessDeniedException) -> escrever(
								response, objectMapper, HttpStatus.FORBIDDEN, "Você não tem permissão para esta operação.")))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/api/status", "/api/auth/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/usuarios/me").authenticated()
						.requestMatchers(HttpMethod.GET, "/api/usuarios", "/api/usuarios/**").hasAnyRole("GESTOR", "ADMIN")
						.requestMatchers("/api/usuarios", "/api/usuarios/**").hasRole("ADMIN")
						.requestMatchers(HttpMethod.GET, "/api/pontos-coleta", "/api/pontos-coleta/**").authenticated()
						.requestMatchers("/api/pontos-coleta", "/api/pontos-coleta/**").hasAnyRole("GESTOR", "ADMIN")
						.requestMatchers(HttpMethod.GET, "/api/tipos-residuo", "/api/tipos-residuo/**").authenticated()
						.requestMatchers("/api/tipos-residuo", "/api/tipos-residuo/**").hasAnyRole("GESTOR", "ADMIN")
						.requestMatchers("/api/descartes", "/api/descartes/**").authenticated()
						.anyRequest().authenticated())
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
				.build();
	}

	@Bean
	JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService, UsuarioRepository usuarioRepository) {
		return new JwtAuthenticationFilter(jwtService, usuarioRepository);
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	UserDetailsService userDetailsService(UsuarioRepository usuarioRepository) {
		return email -> {
			Usuario usuario = usuarioRepository.findByEmail(email)
					.orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado."));
			String senha = usuario.getSenha() == null ? "" : usuario.getSenha();
			return User.withUsername(usuario.getEmail())
					.password(senha)
					.roles(usuario.getTipo().name())
					.build();
		};
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuracao = new CorsConfiguration();
		configuracao.setAllowedOriginPatterns(List.of("*"));
		configuracao.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
		configuracao.setAllowedHeaders(List.of("*"));
		UrlBasedCorsConfigurationSource origem = new UrlBasedCorsConfigurationSource();
		origem.registerCorsConfiguration("/**", configuracao);
		return origem;
	}

	private void escrever(
			HttpServletResponse response,
			ObjectMapper objectMapper,
			HttpStatus status,
			String mensagem
	) throws IOException {
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(response.getOutputStream(), new ErroResponse(
				status.value(),
				status == HttpStatus.UNAUTHORIZED ? "Não autenticado" : "Acesso negado",
				mensagem,
				List.of()
		));
	}

}
