package br.com.fiap.pontegames.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pontegames.jwt")
public record JwtProperties(String segredo, long expiracaoMinutos) {
}
