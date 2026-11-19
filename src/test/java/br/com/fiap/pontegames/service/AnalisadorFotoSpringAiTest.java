package br.com.fiap.pontegames.service;

import br.com.fiap.pontegames.model.ParecerIa;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnalisadorFotoSpringAiTest {

	@Test
	void incompativelNaoViraCompativel() {
		ParecerFoto parecer = AnalisadorFotoSpringAi.interpretar(
				"INCOMPATIVEL A foto mostra uma garrafa de agua, nao oleo.");

		assertThat(parecer.resultado()).isEqualTo(ParecerIa.INCOMPATIVEL);
		assertThat(parecer.mensagem()).contains("garrafa");
	}

	@Test
	void compativelLeAFrase() {
		ParecerFoto parecer = AnalisadorFotoSpringAi.interpretar("COMPATIVEL A foto mostra oleo de cozinha.");

		assertThat(parecer.resultado()).isEqualTo(ParecerIa.COMPATIVEL);
		assertThat(parecer.mensagem()).contains("oleo");
	}

	@Test
	void textoVazioFalha() {
		assertThatThrownBy(() -> AnalisadorFotoSpringAi.interpretar("  "))
				.isInstanceOf(IllegalStateException.class);
	}

}
