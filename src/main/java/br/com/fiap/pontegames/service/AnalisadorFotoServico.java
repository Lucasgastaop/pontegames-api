package br.com.fiap.pontegames.service;

import br.com.fiap.pontegames.model.ParecerIa;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Locale;

@Service
@Profile("!test")
public class AnalisadorFotoServico implements AnalisadorFoto {

	private static final Logger LOG = LoggerFactory.getLogger(AnalisadorFotoServico.class);
	private static final Duration ESPERA = Duration.ofSeconds(60);

	private final RestClient cliente;
	private final ObjectMapper json;
	private final String urlServico;
	private final AnalisadorFotoSpringAi reserva;

	public AnalisadorFotoServico(
			@Value("${pontegames.ia.url}") String urlServico,
			ObjectMapper json,
			AnalisadorFotoSpringAi reserva
	) {
		this.urlServico = urlServico;
		this.json = json;
		this.reserva = reserva;
		HttpClient http = HttpClient.newBuilder()
				.connectTimeout(ESPERA)
				.followRedirects(HttpClient.Redirect.NORMAL)
				.build();
		org.springframework.http.client.JdkClientHttpRequestFactory fabrica =
				new org.springframework.http.client.JdkClientHttpRequestFactory(http);
		fabrica.setReadTimeout(ESPERA);
		this.cliente = RestClient.builder().requestFactory(fabrica).build();
	}

	@Override
	public ParecerFoto analisar(String nomeResiduo, String instrucao, String fotoUrl) {
		try {
			byte[] imagem = baixar(fotoUrl);
			if (imagem != null && imagem.length > 0) {
				RespostaServicoIa resposta = consultar(imagem, nome(fotoUrl));
				if (resposta != null && resposta.valido() != null) {
					return traduzir(resposta, nomeResiduo);
				}
			}
		} catch (RuntimeException ex) {
			LOG.warn("Serviço de IA indisponível, usando o modelo local: {}", ex.getMessage());
		}
		return reserva.analisar(nomeResiduo, instrucao, fotoUrl);
	}

	@Override
	public ParecerFoto analisar(String nomeResiduo, String instrucao, byte[] imagem, String nomeArquivo) {
		try {
			if (imagem != null && imagem.length > 0) {
				RespostaServicoIa resposta = consultar(imagem, nomeArquivo == null ? "foto.jpg" : nomeArquivo);
				if (resposta != null && resposta.valido() != null) {
					return traduzir(resposta, nomeResiduo);
				}
			}
		} catch (RuntimeException ex) {
			LOG.warn("Serviço de IA indisponível, usando o modelo local: {}", ex.getMessage());
		}
		return reserva.analisar(nomeResiduo, instrucao, imagem, nomeArquivo);
	}

	private byte[] baixar(String fotoUrl) {
		URI uri = uriHttp(fotoUrl);
		if (uri == null) {
			return null;
		}
		return cliente.get().uri(uri).retrieve().body(byte[].class);
	}

	private RespostaServicoIa consultar(byte[] imagem, String nomeArquivo) {
		MultiValueMap<String, Object> corpo = new LinkedMultiValueMap<>();
		HttpHeaders arquivo = new HttpHeaders();
		arquivo.setContentType(FormatoFoto.png(imagem) ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG);
		corpo.add("foto", new HttpEntity<>(new ByteArrayResource(imagem) {
			@Override
			public String getFilename() {
				return nomeArquivo;
			}
		}, arquivo));
		try {
			return postar(corpo);
		} catch (RestClientResponseException ex) {
			int status = ex.getStatusCode().value();
			if (status != 502 && status != 503) {
				throw ex;
			}
			LOG.warn("Serviço de IA hibernando (HTTP {}). Nova tentativa.", status);
			esperar();
			return postar(corpo);
		}
	}

	private RespostaServicoIa postar(MultiValueMap<String, Object> corpo) {
		String resposta = cliente.post()
				.uri(urlServico)
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.body(corpo)
				.retrieve()
				.body(String.class);
		if (resposta == null || resposta.isBlank()) {
			return null;
		}
		JsonNode no = json.readTree(resposta);
		if (!no.hasNonNull("valido")) {
			return null;
		}
		return new RespostaServicoIa(
				no.get("valido").asBoolean(),
				texto(no.path("categoria").asString(null)),
				texto(no.path("material_detectado").asString(null)),
				texto(no.path("motivo_rejeicao").asString(null))
		);
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

	private void esperar() {
		try {
			Thread.sleep(Duration.ofSeconds(15));
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
