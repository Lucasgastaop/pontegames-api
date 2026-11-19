package br.com.fiap.pontegames.service;

import br.com.fiap.pontegames.model.ParecerIa;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

final class FotoMinima {

	static final int LADO_MINIMO = 400;

	private FotoMinima() {
	}

	static ParecerFoto avaliar(byte[] imagem, String nomeResiduo) {
		if (imagem == null || imagem.length == 0) {
			return null;
		}
		try {
			BufferedImage foto = ImageIO.read(new ByteArrayInputStream(imagem));
			if (foto == null) {
				return null;
			}
			int menor = Math.min(foto.getWidth(), foto.getHeight());
			if (menor >= LADO_MINIMO) {
				return null;
			}
			String nome = nomeResiduo == null || nomeResiduo.isBlank() ? "resíduo declarado" : nomeResiduo;
			return new ParecerFoto(
					ParecerIa.INCOMPATIVEL,
					"A foto precisa mostrar o " + nome + " de perto e nítido. Imagem pequena, print ou ícone não confirma o descarte."
			);
		} catch (IOException ex) {
			return null;
		}
	}

}
