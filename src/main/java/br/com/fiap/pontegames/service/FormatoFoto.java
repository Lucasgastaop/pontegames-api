package br.com.fiap.pontegames.service;

final class FormatoFoto {

	private FormatoFoto() {
	}

	static boolean jpeg(byte[] conteudo) {
		return conteudo != null
				&& conteudo.length >= 3
				&& (conteudo[0] & 0xFF) == 0xFF
				&& (conteudo[1] & 0xFF) == 0xD8
				&& (conteudo[2] & 0xFF) == 0xFF;
	}

	static boolean png(byte[] conteudo) {
		return conteudo != null
				&& conteudo.length >= 8
				&& (conteudo[0] & 0xFF) == 0x89
				&& conteudo[1] == 0x50
				&& conteudo[2] == 0x4E
				&& conteudo[3] == 0x47
				&& conteudo[4] == 0x0D
				&& conteudo[5] == 0x0A
				&& conteudo[6] == 0x1A
				&& conteudo[7] == 0x0A;
	}

}
