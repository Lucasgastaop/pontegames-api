package br.com.fiap.pontegames.service;

import br.com.fiap.pontegames.model.ParecerIa;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;

@Service
@Profile("!test")
public class AnalisadorFotoServico implements AnalisadorFoto {

	private static final Logger LOG = LoggerFactory.getLogger(AnalisadorFotoServico.class);
	private static final Duration DOWNLOAD = Duration.ofSeconds(8);
	private static final Duration CONEXAO = Duration.ofSeconds(30);
	private static final Duration ESPERA = Duration.ofSeconds(80);
	private static final String LIMITE = "PonteGamesBoundary";

	private final HttpClient http;
	private final ObjectMapper json;
	private final String urlServico;
	private final AnalisadorFotoSpringAi reserva;
	private final boolean preferirSpring;

	public AnalisadorFotoServico(
			@Value("${pontegames.ia.url}") String urlServico,
			@Value("${pontegames.ia.preferir-spring:false}") boolean preferirSpring,
			ObjectMapper json,
			AnalisadorFotoSpringAi reserva
	) {
		this.urlServico = urlServico;
		this.preferirSpring = preferirSpring;
		this.json = json;
		this.reserva = reserva;
		this.http = HttpClient.newBuilder()
				.connectTimeout(CONEXAO)
				.followRedirects(HttpClient.Redirect.NORMAL)
				.build();
	}

	@Override
	public ParecerFoto analisar(String nomeResiduo, String instrucao, String fotoUrl) {
		byte[] imagem = baixar(fotoUrl);
		ParecerFoto fraca = FotoMinima.avaliar(imagem, nomeResiduo);
		if (fraca != null) {
			return fraca;
		}
		return decidir(nomeResiduo, instrucao, imagem, nome(fotoUrl), fotoUrl);
	}

	@Override
	public ParecerFoto analisar(String nomeResiduo, String instrucao, byte[] imagem, String nomeArquivo) {
		ParecerFoto fraca = FotoMinima.avaliar(imagem, nomeResiduo);
		if (fraca != null) {
			return fraca;
		}
		String arquivo = nomeArquivo == null ? "foto.jpg" : nomeArquivo;
		return decidir(nomeResiduo, instrucao, imagem, arquivo, null);
	}

	private ParecerFoto decidir(
			String nomeResiduo,
			String instrucao,
			byte[] imagem,
			String nomeArquivo,
			String fotoUrl
	) {
		boolean temImagem = imagem != null && imagem.length > 0;
		if (temImagem) {
			try {
				return parecerGrupo(imagem, nomeArquivo, nomeResiduo);
			} catch (RuntimeException ex) {
				LOG.warn("Serviço de IA indisponível, usando o Spring AI: {}", ex.toString());
			}
		} else if (preferirSpring) {
			try {
				return parecerSpring(nomeResiduo, instrucao, imagem, nomeArquivo, fotoUrl);
			} catch (RuntimeException ex) {
				LOG.warn("Spring AI indisponível: {}", ex.toString());
				throw new IllegalStateException("O Spring AI e o serviço do grupo não analisaram a foto.");
			}
		}
		return parecerSpring(nomeResiduo, instrucao, imagem, nomeArquivo, fotoUrl);
	}

	private ParecerFoto parecerGrupo(byte[] imagem, String nomeArquivo, String nomeResiduo) {
		RespostaServicoIa resposta = consultar(imagem, nomeArquivo);
		if (resposta == null || resposta.valido() == null) {
			throw new IllegalStateException("O serviço de IA não devolveu um parecer.");
		}
		return traduzir(resposta, nomeResiduo);
	}

	private ParecerFoto parecerSpring(
			String nomeResiduo,
			String instrucao,
			byte[] imagem,
			String nomeArquivo,
			String fotoUrl
	) {
		if (imagem != null && imagem.length > 0) {
			return reserva.analisar(nomeResiduo, instrucao, imagem, nomeArquivo);
		}
		if (fotoUrl != null) {
			return reserva.analisar(nomeResiduo, instrucao, fotoUrl);
		}
		return reserva.analisar(nomeResiduo, instrucao, imagem, nomeArquivo);
	}

	private byte[] baixar(String fotoUrl) {
		URI uri = uriHttp(fotoUrl);
		if (uri == null) {
			return null;
		}
		try {
			HttpRequest pedido = HttpRequest.newBuilder(uri)
					.timeout(DOWNLOAD)
					.header("User-Agent", "PonteGames/1.0")
					.GET()
					.build();
			HttpResponse<byte[]> resposta = http.send(pedido, HttpResponse.BodyHandlers.ofByteArray());
			if (resposta.statusCode() < 200 || resposta.statusCode() >= 300) {
				LOG.warn("Download da foto respondeu HTTP {}.", resposta.statusCode());
				return null;
			}
			return resposta.body();
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			LOG.warn("Download da foto interrompido.");
			return null;
		} catch (IOException ex) {
			LOG.warn("Não foi possível baixar a foto para o parecer: {}", ex.toString());
			return null;
		}
	}

	private RespostaServicoIa consultar(byte[] imagem, String nomeArquivo) {
		boolean png = FormatoFoto.png(imagem);
		String tipo = png ? "image/png" : "image/jpeg";
		String arquivo = nomeArquivo == null || nomeArquivo.isBlank() ? (png ? "foto.png" : "foto.jpg") : nomeArquivo;
		byte[] corpo = multipart(imagem, arquivo, tipo);
		long limite = System.nanoTime() + ESPERA.toNanos();
		try {
			return enviar(corpo);
		} catch (IOException ex) {
			if (limite - System.nanoTime() < Duration.ofSeconds(25).toNanos()) {
				throw new IllegalStateException("O serviço de IA não respondeu.", ex);
			}
			LOG.warn("Serviço de IA indisponível ({}). Nova tentativa.", ex.toString());
			esperar(Duration.ofSeconds(5));
			try {
				return enviar(corpo);
			} catch (IOException segunda) {
				throw new IllegalStateException("O serviço de IA não respondeu.", segunda);
			} catch (InterruptedException segunda) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException("A espera pelo serviço de IA foi interrompida.");
			}
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("A espera pelo serviço de IA foi interrompida.");
		}
	}

	private RespostaServicoIa enviar(byte[] corpo) throws IOException, InterruptedException {
		HttpRequest pedido = HttpRequest.newBuilder(URI.create(urlServico))
				.timeout(ESPERA)
				.header("User-Agent", "PonteGames/1.0")
				.header("Content-Type", "multipart/form-data; boundary=" + LIMITE)
				.POST(HttpRequest.BodyPublishers.ofByteArray(corpo))
				.build();
		HttpResponse<String> resposta = http.send(pedido, HttpResponse.BodyHandlers.ofString());
		int status = resposta.statusCode();
		String texto = resposta.body() == null ? "" : resposta.body();
		if (status == 502 || status == 503 || status == 504) {
			throw new IOException("HTTP " + status);
		}
		if (status < 200 || status >= 300) {
			throw new IllegalStateException("Serviço de IA respondeu HTTP " + status + ".");
		}
		if (texto.isBlank()) {
			return null;
		}
		JsonNode no = json.readTree(texto);
		if (!no.hasNonNull("valido")) {
			throw new IllegalStateException("O serviço de IA não devolveu um parecer.");
		}
		return new RespostaServicoIa(
				no.get("valido").asBoolean(),
				texto(no.path("categoria").asString(null)),
				texto(no.path("material_detectado").asString(null)),
				texto(no.path("motivo_rejeicao").asString(null))
		);
	}

	private byte[] multipart(byte[] imagem, String nomeArquivo, String tipo) {
		String nome = nomeArquivo.replace("\"", "");
		byte[] inicio = ("--" + LIMITE + "\r\n"
				+ "Content-Disposition: form-data; name=\"foto\"; filename=\"" + nome + "\"\r\n"
				+ "Content-Type: " + tipo + "\r\n\r\n").getBytes(StandardCharsets.UTF_8);
		byte[] fim = ("\r\n--" + LIMITE + "--\r\n").getBytes(StandardCharsets.UTF_8);
		byte[] corpo = new byte[inicio.length + imagem.length + fim.length];
		System.arraycopy(inicio, 0, corpo, 0, inicio.length);
		System.arraycopy(imagem, 0, corpo, inicio.length, imagem.length);
		System.arraycopy(fim, 0, corpo, inicio.length + imagem.length, fim.length);
		return corpo;
	}

	private ParecerFoto traduzir(RespostaServicoIa resposta, String nomeResiduo) {
		if (Boolean.TRUE.equals(resposta.valido())) {
			String material = texto(resposta.materialDetectado());
			String categoria = texto(resposta.categoria());
			String mensagem = material == null
					? "A foto parece compatível com " + nomeResiduo + "."
					: "A foto indica " + material + (categoria == null ? "." : " (" + categoria + ").");
			return new ParecerFoto(ParecerIa.COMPATIVEL, mensagem);
		}
		String motivo = texto(resposta.motivoRejeicao());
		if (motivo == null) {
			motivo = "A foto não parece ser " + nomeResiduo + ".";
		}
		return new ParecerFoto(ParecerIa.INCOMPATIVEL, motivo);
	}

	private void esperar(Duration pausa) {
		try {
			Thread.sleep(pausa);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("A espera pelo serviço de IA foi interrompida.");
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

	private String nome(String fotoUrl) {
		String caminho = fotoUrl.toLowerCase(Locale.ROOT);
		if (caminho.contains(".png")) {
			return "foto.png";
		}
		if (caminho.contains(".webp")) {
			return "foto.webp";
		}
		if (caminho.contains(".gif")) {
			return "foto.gif";
		}
		return "foto.jpg";
	}

	private String texto(String valor) {
		if (valor == null || valor.isBlank()) {
			return null;
		}
		return valor.trim();
	}

	private record RespostaServicoIa(
			Boolean valido,
			String categoria,
			String materialDetectado,
			String motivoRejeicao
	) {
	}

}
