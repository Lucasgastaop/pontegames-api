package br.com.fiap.pontegames.service;

import br.com.fiap.pontegames.model.ParecerIa;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("test")
public class AnalisadorFotoFixa implements AnalisadorFoto {

	@Override
	public ParecerFoto analisar(String nomeResiduo, String instrucao, String fotoUrl) {
		String url = fotoUrl.toLowerCase();
		if (url.contains("falha")) {
			throw new IllegalStateException("modelo indisponível");
		}
		if (url.contains("incompativel")) {
			return new ParecerFoto(ParecerIa.INCOMPATIVEL, "A foto não parece ser " + nomeResiduo + ".");
		}
		return new ParecerFoto(ParecerIa.COMPATIVEL, "A foto parece compatível com " + nomeResiduo + ".");
	}

}
