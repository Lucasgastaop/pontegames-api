package br.com.fiap.pontegames.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AutenticacaoAtual {

	public UsuarioAutenticado exigir() {
		Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
		if (autenticacao == null || !(autenticacao.getPrincipal() instanceof UsuarioAutenticado atual)) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autenticação necessária.");
		}
		return atual;
	}

}
