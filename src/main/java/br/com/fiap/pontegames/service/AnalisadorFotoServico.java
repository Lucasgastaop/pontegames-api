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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

@Service
@Profile("!test")
public class AnalisadorFotoServico implements AnalisadorFoto {

	private static final Logger LOG = LoggerFactory.getLogger(AnalisadorFotoServico.class);
	private static final Duration DOWNLOAD = Duration.ofSeconds(4);
	private static final Duration ESPERA = Duration.ofSeconds(16);

	private final RestClient cliente;
	private final RestClient download;
	private final ObjectMapper json;
	private final String urlServico;
	private final AnalisadorFotoSpringAi reserva;
	private final boolean preferirSpring;
	private final ExecutorService tarefas = Executors.newVirtualThreadPerTaskExecutor();

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
		this.cliente = clienteHttp(ESPERA);
		this.download = clienteHttp(DOWNLOAD);
	}

	@Override
	public ParecerFoto analisar(String nomeResiduo, String instrucao, String fotoUrl) {
		byte[] imagem = null;
		try {
			imagem = baixar(fotoUrl);
		} catch (Exception ex) {
			LOG.warn("Não foi possível baixar a foto para o parecer: {}", ex.getMessage());
		}
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
			return junto(nomeResiduo, instrucao, imagem, nomeArquivo, fotoUrl);
		}
		if (preferirSpring) {
			try {
				return parecerSpring(nomeResiduo, instrucao, imagem, nomeArquivo, fotoUrl);
			} catch (Exception ex) {
				LOG.warn("Spring AI indisponível: {}", ex.getMessage());
				throw new IllegalStateException("O Spring AI e o serviço do grupo não analisaram a foto.");
			}
		}
		return parecerSpring(nomeResiduo, instrucao, imagem, nomeArquivo, fotoUrl);
	}

	private ParecerFoto junto(
			String nomeResiduo,
			String instrucao,
			byte[] imagem,
			String nomeArquivo,
			String fotoUrl
	) {
		CompletableFuture<ParecerFoto> spring = CompletableFuture.supplyAsync(
				() -> parecerSpring(nomeResiduo, instrucao, imagem, nomeArquivo, fotoUrl), tarefas);
		CompletableFuture<ParecerFoto> grupo = CompletableFuture.supplyAsync(
				() -> parecerGrupo(imagem, nomeArquivo, nomeResiduo), tarefas);
		CompletableFuture<ParecerFoto> primeiro = new CompletableFuture<>();
		AtomicInteger falhas = new AtomicInteger();
		BiConsumer<ParecerFoto, Throwable> aoEncerrar = (parecer, erro) -> {
			if (erro == null && parecer != null) {
				primeiro.complete(parecer);
				return;
			}
			if (falhas.incrementAndGet() == 2) {
				primeiro.completeExceptionally(erro == null
						? new IllegalStateException("O Spring AI e o serviço do grupo não analisaram a foto.")
						: erro);
			}
		};
		spring.whenComplete(aoEncerrar);
		grupo.whenComplete(aoEncerrar);
		try {
			ParecerFoto parecer = primeiro.get(ESPERA.toMillis(), TimeUnit.MILLISECONDS);
			spring.cancel(true);
			grupo.cancel(true);
			return parecer;
		} catch (TimeoutException ex) {
			spring.cancel(true);
			grupo.cancel(true);
			LOG.warn("Parecer da foto interrompido depois de {} segundos.", ESPERA.toSeconds());
			throw new IllegalStateException("O parecer da foto passou de " + ESPERA.toSeconds() + " segundos.");
		} catch (InterruptedException ex) {
			spring.cancel(true);
			grupo.cancel(true);
			Thread.currentThread().interrupt();
			throw new IllegalStateException("A espera pelo parecer foi interrompida.");
		} catch (ExecutionException ex) {
			spring.cancel(true);
			grupo.cancel(true);
			LOG.warn("Spring AI e o serviço do grupo não analisaram a foto: {}", mensagem(ex));
			throw new IllegalStateException("O Spring AI e o serviço do grupo não analisaram a foto.");
		}
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
		return download.get().uri(uri).retrieve().body(byte[].class);
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
		return postar(corpo);
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

	private static RestClient clienteHttp(Duration leitura) {
		HttpClient http = HttpClient.newBuilder()
				.connectTimeout(DOWNLOAD)
				.followRedirects(HttpClient.Redirect.NORMAL)
				.build();
		org.springframework.http.client.JdkClientHttpRequestFactory fabrica =
				new org.springframework.http.client.JdkClientHttpRequestFactory(http);
		fabrica.setReadTimeout(leitura);
		return RestClient.builder().requestFactory(fabrica).build();
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

	private String mensagem(Exception ex) {
		Throwable causa = ex instanceof ExecutionException && ex.getCause() != null ? ex.getCause() : ex;
		String texto = causa.getMessage();
		if (texto == null || texto.isBlank()) {
			return causa.toString();
		}
		return texto;
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
