package br.com.fiap.pontegames.service;

import br.com.fiap.pontegames.model.ParecerIa;

final class FotoMinima {

	static final int LADO_MINIMO = 400;

	private FotoMinima() {
	}

	static ParecerFoto avaliar(byte[] imagem, String nomeResiduo) {
		if (imagem == null || imagem.length == 0) {
			return null;
		}
		try {
			int[] lados = dimensoes(imagem);
			if (lados == null) {
				return null;
			}
			int menor = Math.min(lados[0], lados[1]);
			if (menor >= LADO_MINIMO) {
				return null;
			}
			String nome = nomeResiduo == null || nomeResiduo.isBlank() ? "resíduo declarado" : nomeResiduo;
			return new ParecerFoto(
					ParecerIa.INCOMPATIVEL,
					"A foto precisa mostrar o " + nome + " de perto e nítido. Imagem pequena, print ou ícone não confirma o descarte."
			);
		} catch (Throwable ex) {
			return null;
		}
	}

	private static int[] dimensoes(byte[] dados) {
		if (png(dados)) {
			return dimensoesPng(dados);
		}
		if (jpeg(dados)) {
			return dimensoesJpeg(dados);
		}
		return null;
	}

	private static boolean jpeg(byte[] dados) {
		return dados.length > 3 && (dados[0] & 0xFF) == 0xFF && (dados[1] & 0xFF) == 0xD8;
	}

	private static boolean png(byte[] dados) {
		return dados.length > 8
				&& (dados[0] & 0xFF) == 0x89
				&& dados[1] == 0x50
				&& dados[2] == 0x4E
				&& dados[3] == 0x47;
	}

	private static int[] dimensoesPng(byte[] dados) {
		if (dados.length < 24) {
			return null;
		}
		int largura = inteiro(dados, 16);
		int altura = inteiro(dados, 20);
		if (largura <= 0 || altura <= 0) {
			return null;
		}
		return new int[] {largura, altura};
	}

	private static int[] dimensoesJpeg(byte[] dados) {
		int indice = 2;
		while (indice + 8 < dados.length) {
			if ((dados[indice] & 0xFF) != 0xFF) {
				indice++;
				continue;
			}
			int marcador = dados[indice + 1] & 0xFF;
			while (marcador == 0xFF && indice + 2 < dados.length) {
				indice++;
				marcador = dados[indice + 1] & 0xFF;
			}
			if (marcador == 0xD8 || marcador == 0xD9 || marcador == 0x01 || (marcador >= 0xD0 && marcador <= 0xD7)) {
				indice += 2;
				continue;
			}
			if (indice + 3 >= dados.length) {
				return null;
			}
			int tamanho = ((dados[indice + 2] & 0xFF) << 8) | (dados[indice + 3] & 0xFF);
			if (tamanho < 2 || indice + 2 + tamanho > dados.length) {
				return null;
			}
			if (marcadorDeQuadro(marcador)) {
				int altura = ((dados[indice + 5] & 0xFF) << 8) | (dados[indice + 6] & 0xFF);
				int largura = ((dados[indice + 7] & 0xFF) << 8) | (dados[indice + 8] & 0xFF);
				if (largura <= 0 || altura <= 0) {
					return null;
				}
				return new int[] {largura, altura};
			}
			indice += 2 + tamanho;
		}
		return null;
	}

	private static boolean marcadorDeQuadro(int marcador) {
		return marcador >= 0xC0 && marcador <= 0xCF && marcador != 0xC4 && marcador != 0xC8 && marcador != 0xCC;
	}

	private static int inteiro(byte[] dados, int inicio) {
		return ((dados[inicio] & 0xFF) << 24)
				| ((dados[inicio + 1] & 0xFF) << 16)
				| ((dados[inicio + 2] & 0xFF) << 8)
				| (dados[inicio + 3] & 0xFF);
	}

}
