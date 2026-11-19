package br.com.fiap.pontegames.service;

import br.com.fiap.pontegames.model.ParecerIa;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import java.net.MalformedURLException;
import java.net.URI;
import java.text.Normalizer;
import java.util.Locale;

@Component
@Profile("!test")
class AnalisadorFotoSpringAi {

	private static final String INSTRUCAO = """
			Você confere se a foto prova um descarte correto do resíduo declarado.
			A primeira palavra da resposta deve ser exatamente uma destas: COMPATIVEL, INCOMPATIVEL, INCONCLUSIVO.
			Use COMPATIVEL só quando a imagem é uma foto real, o resíduo declarado é o assunto principal e dá para reconhecê-lo com clareza: óleo de cozinha em garrafa ou recipiente, aparelho ou peça eletrônica, ou pilha e bateria.
			Use INCOMPATIVEL quando for outro material, lixo misturado sem o item declarado, print, desenho, logo, cartaz, paisagem, prédios ou uma pessoa sem o resíduo.
			Use INCONCLUSIVO quando a foto está escura, tremida, longe ou cortada e o material não dá para confirmar.
			Depois da palavra, escreva uma frase em português, com no máximo 240 caracteres, dizendo ao cidadão o que a foto indica.
			""";

	private final ChatClient chatClient;
	private final boolean render;
	private final String baseUrl;

	public AnalisadorFotoSpringAi(
			ChatClient.Builder builder,
			@Value("${RENDER:false}") boolean render,
			@Value("${spring.ai.openai.base-url:}") String baseUrl
	) {
		this.chatClient = builder.build();
		this.render = render;
		this.baseUrl = baseUrl;
	}

	ParecerFoto analisar(String nomeResiduo, String instrucao, String fotoUrl) {
		exigirModeloAlcancavel();
		String pedido = """
				Tipo declarado: %s.
				Como esse resíduo deve ser entregue: %s.
				""".formatted(nomeResiduo, instrucao);
		String texto = chatClient.prompt()
				.system(INSTRUCAO)
				.user(usuario -> preencher(usuario, pedido, fotoUrl))
				.call()
				.content();
		return interpretar(texto);
	}

	ParecerFoto analisar(String nomeResiduo, String instrucao, byte[] imagem, String nomeArquivo) {
		exigirModeloAlcancavel();
		String pedido = """
				Tipo declarado: %s.
				Como esse resíduo deve ser entregue: %s.
				""".formatted(nomeResiduo, instrucao);
		String texto = chatClient.prompt()
				.system(INSTRUCAO)
				.user(usuario -> {
					if (imagem == null || imagem.length == 0) {
						usuario.text(pedido);
						return;
					}
					String nome = nomeArquivo == null ? "foto.jpg" : nomeArquivo;
					usuario.text(pedido).media(mime(nome), new ByteArrayResource(imagem));
				})
				.call()
				.content();
		return interpretar(texto);
	}

	private void exigirModeloAlcancavel() {
		String endereco = baseUrl == null ? "" : baseUrl.toLowerCase(Locale.ROOT);
		boolean local = endereco.isBlank() || endereco.contains("localhost") || endereco.contains("127.0.0.1");
		if (render && local) {
			throw new IllegalStateException(
					"No Render o Spring AI precisa de OPENAI_BASE_URL, OPENAI_API_KEY e OPENAI_MODEL de um modelo com visão.");
		}
	}

	private void preencher(ChatClient.PromptUserSpec usuario, String texto, String fotoUrl) {
		URI uri = uriHttp(fotoUrl);
		if (uri == null) {
			usuario.text(texto);
			return;
		}
		try {
			usuario.text(texto).media(mime(fotoUrl), uri.toURL());
		} catch (MalformedURLException ex) {
			usuario.text(texto);
		}
	}

	private URI uriHttp(String fotoUrl) {
		try {
			URI uri = URI.create(fotoUrl);
			String esquema = uri.getScheme();
			if (esquema == null) {
				return null;
			}
			if (!esquema.equalsIgnoreCase("http") && !esquema.equalsIgnoreCase("https")) {
				return null;
			}
			return uri;
		} catch (IllegalArgumentException ex) {
			return null;
		}
	}

	private MimeType mime(String fotoUrl) {
		String caminho = fotoUrl.toLowerCase(Locale.ROOT);
		if (caminho.contains(".png")) {
			return MimeTypeUtils.IMAGE_PNG;
		}
		if (caminho.contains(".gif")) {
			return MimeTypeUtils.IMAGE_GIF;
		}
		if (caminho.contains(".webp")) {
			return MimeTypeUtils.parseMimeType("image/webp");
		}
		return MimeTypeUtils.IMAGE_JPEG;
	}

	static ParecerFoto interpretar(String texto) {
		if (texto == null || texto.isBlank()) {
			throw new IllegalStateException("O modelo não devolveu um parecer.");
		}
		String chave = Normalizer.normalize(texto, Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "")
				.toUpperCase(Locale.ROOT);
		ParecerIa resultado = ParecerIa.INCONCLUSIVO;
		String palavra = null;
		if (chave.contains("INCOMPATIVEL")) {
			resultado = ParecerIa.INCOMPATIVEL;
			palavra = "INCOMPATIVEL";
		} else if (chave.contains("COMPATIVEL")) {
			resultado = ParecerIa.COMPATIVEL;
			palavra = "COMPATIVEL";
		} else if (chave.contains("INCONCLUSIVO")) {
			palavra = "INCONCLUSIVO";
		}
		return new ParecerFoto(resultado, mensagem(texto, chave, palavra));
	}

	private static String mensagem(String original, String normalizado, String palavra) {
		if (palavra == null) {
			return null;
		}
		String maiusculo = original.toUpperCase(Locale.ROOT);
		int inicio = maiusculo.indexOf(palavra);
		String base = original;
		if (inicio < 0) {
			inicio = normalizado.indexOf(palavra);
			base = normalizado;
		}
		if (inicio < 0) {
			return null;
		}
		String resto = base.substring(inicio + palavra.length()).replaceAll("[^\\p{L}\\p{N}\\s]", " ").replaceAll("\\s+", " ").trim();
		if (resto.isBlank()) {
			return null;
		}
		return resto.length() <= 240 ? resto : resto.substring(0, 240).trim();
	}

}
