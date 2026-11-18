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
			Você confere a foto de um descarte feito por um cidadão num ecoponto.
			Compare a imagem com o tipo de resíduo declarado.
			resultado deve ser exatamente um destes valores: COMPATIVEL, INCOMPATIVEL, INCONCLUSIVO.
			Use COMPATIVEL quando a foto mostra o resíduo declarado.
			Use INCOMPATIVEL quando a foto mostra outra coisa.
			Use INCONCLUSIVO quando a imagem não dá para decidir.
			mensagem é uma frase em português, com no máximo 240 caracteres, dizendo ao cidadão o que a foto indica.
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
		ParecerModelo modelo = chatClient.prompt()
				.system(INSTRUCAO)
				.user(usuario -> preencher(usuario, pedido, fotoUrl))
				.call()
				.entity(ParecerModelo.class);
		if (modelo == null) {
			throw new IllegalStateException("O modelo não devolveu um parecer.");
		}
		return new ParecerFoto(ler(modelo.resultado()), modelo.mensagem());
	}

	ParecerFoto analisar(String nomeResiduo, String instrucao, byte[] imagem, String nomeArquivo) {
		exigirModeloAlcancavel();
		String pedido = """
				Tipo declarado: %s.
				Como esse resíduo deve ser entregue: %s.
				""".formatted(nomeResiduo, instrucao);
		ParecerModelo modelo = chatClient.prompt()
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
				.entity(ParecerModelo.class);
		if (modelo == null) {
			throw new IllegalStateException("O modelo não devolveu um parecer.");
		}
		return new ParecerFoto(ler(modelo.resultado()), modelo.mensagem());
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

	private ParecerIa ler(String valor) {
		if (valor == null || valor.isBlank()) {
			return ParecerIa.INCONCLUSIVO;
		}
		String chave = Normalizer.normalize(valor, Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "")
				.toUpperCase(Locale.ROOT)
				.trim();
		return switch (chave) {
			case "COMPATIVEL" -> ParecerIa.COMPATIVEL;
			case "INCOMPATIVEL" -> ParecerIa.INCOMPATIVEL;
			default -> ParecerIa.INCONCLUSIVO;
		};
	}

}
