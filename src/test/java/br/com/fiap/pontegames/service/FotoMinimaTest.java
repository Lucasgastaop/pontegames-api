package br.com.fiap.pontegames.service;

import br.com.fiap.pontegames.model.ParecerIa;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class FotoMinimaTest {

	@Test
	void imagemPequenaNaoConfirmaODescarte() throws IOException {
		ParecerFoto parecer = FotoMinima.avaliar(jpeg(120, 80), "Óleo de Cozinha");

		assertThat(parecer).isNotNull();
		assertThat(parecer.resultado()).isEqualTo(ParecerIa.INCOMPATIVEL);
		assertThat(parecer.mensagem()).contains("Óleo de Cozinha");
	}

	@Test
	void fotoGrandeSegueParaAAnalise() throws IOException {
		assertThat(FotoMinima.avaliar(jpeg(800, 600), "Pilha")).isNull();
	}

	private byte[] jpeg(int largura, int altura) throws IOException {
		BufferedImage imagem = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_RGB);
		ByteArrayOutputStream saida = new ByteArrayOutputStream();
		ImageIO.write(imagem, "jpg", saida);
		return saida.toByteArray();
	}

}
