package br.com.fiap.pontegames.security;

import br.com.fiap.pontegames.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

	private final JwtProperties properties;
	private final SecretKey chave;

	public JwtService(JwtProperties properties) {
		if (properties.segredo() == null || properties.segredo().getBytes(StandardCharsets.UTF_8).length < 32) {
			throw new IllegalStateException("pontegames.jwt.segredo precisa ter pelo menos 32 bytes.");
		}
		this.properties = properties;
		this.chave = Keys.hmacShaKeyFor(properties.segredo().getBytes(StandardCharsets.UTF_8));
	}

	public String gerar(Usuario usuario) {
		Instant agora = Instant.now();
		Instant expira = agora.plusSeconds(properties.expiracaoMinutos() * 60);
		return Jwts.builder()
				.subject(usuario.getId().toString())
				.claim("email", usuario.getEmail())
				.claim("tipo", usuario.getTipo().name())
				.issuedAt(Date.from(agora))
				.expiration(Date.from(expira))
				.signWith(chave)
				.compact();
	}

	public Claims ler(String token) {
		return Jwts.parser()
				.verifyWith(chave)
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}

	public boolean valido(String token) {
		try {
			ler(token);
			return true;
		} catch (JwtException | IllegalArgumentException ex) {
			return false;
		}
	}

}
