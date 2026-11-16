package br.com.fiap.pontegames.service;

import br.com.fiap.pontegames.model.TipoResiduo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.Locale;

@Service
public class GamificacaoService {

	public BigDecimal calcularXp(TipoResiduo tipo, BigDecimal pesoKg) {
		return pesoKg.multiply(tipo.getXpPorKg()).setScale(2, RoundingMode.HALF_UP);
	}

	public int nivel(BigDecimal xp) {
		BigDecimal saldo = xp == null || xp.signum() < 0 ? BigDecimal.ZERO : xp;
		if (saldo.compareTo(new BigDecimal("200")) < 0) {
			return 1;
		}
		if (saldo.compareTo(new BigDecimal("500")) < 0) {
			return 2;
		}
		if (saldo.compareTo(new BigDecimal("1000")) < 0) {
			return 3;
		}
		if (saldo.compareTo(new BigDecimal("2000")) < 0) {
			return 4;
		}
		return 5;
	}

	public BigDecimal limiteDiarioKg(TipoResiduo tipo) {
		String chave = semAcento(tipo.getNome());
		if (chave.contains("OLEO")) {
			return new BigDecimal("5");
		}
		if (chave.contains("ELETR")) {
			return new BigDecimal("10");
		}
		if (chave.contains("PILHA") || chave.contains("BATER")) {
			return new BigDecimal("1");
		}
		throw new ResponseStatusException(HttpStatus.CONFLICT, "Tipo de resíduo sem limite diário configurado.");
	}

	private String semAcento(String texto) {
		String normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
		return normalizado.toUpperCase(Locale.ROOT);
	}

}
