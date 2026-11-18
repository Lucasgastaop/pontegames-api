package br.com.fiap.pontegames.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class ArmazenamentoFoto {

	private static final Logger LOG = LoggerFactory.getLogger(ArmazenamentoFoto.class);
	private static final String PREFIXO = "/api/fotos/";
	private static final Pattern NOME = Pattern.compile(
			"[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png)");

	private final Path diretorio;
	private final long tamanhoMaximo;

	public ArmazenamentoFoto(
			@Value("${pontegames.fotos.diretorio:dados/fotos}") String diretorio,
			@Value("${pontegames.fotos.tamanho-maximo-bytes:5242880}") long tamanhoMaximo
	) {
		this.diretorio = Path.of(diretorio).toAbsolutePath().normalize();
		this.tamanhoMaximo = tamanhoMaximo;
		try {
			Files.createDirectories(this.diretorio);
		} catch (IOException ex) {
			throw new IllegalStateException("Não foi possível criar a pasta das fotos.", ex);
		}
	}

	public FotoRecebida ler(MultipartFile foto) {
		if (foto == null || foto.isEmpty()) {
			return null;
		}
		byte[] conteudo = lerBytes(foto);
		if (conteudo.length > tamanhoMaximo) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A foto deve ter no máximo " + limiteLegivel() + ".");
		}
		String extensao = extensao(conteudo);
		return new FotoRecebida(conteudo, nomeArquivo(foto.getOriginalFilename(), extensao));
	}

	public String guardar(FotoRecebida foto) {
		String nome = UUID.randomUUID() + extensao(foto.conteudo());
		try {
			Files.write(diretorio.resolve(nome), foto.conteudo());
		} catch (IOException ex) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Não foi possível guardar a foto.");
		}
		return PREFIXO + nome;
	}

	public FotoGuardada abrir(String nome) {
		if (nome == null || !NOME.matcher(nome).matches()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Foto não encontrada.");
		}
		Path arquivo = diretorio.resolve(nome).normalize();
		if (!arquivo.startsWith(diretorio) || !Files.isRegularFile(arquivo)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Foto não encontrada.");
		}
		MediaType tipo = nome.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
		return new FotoGuardada(nome, new FileSystemResource(arquivo), tipo);
	}

	public void apagarSeNossa(String fotoUrl) {
		if (fotoUrl == null || !fotoUrl.startsWith(PREFIXO)) {
			return;
		}
		String nome = fotoUrl.substring(PREFIXO.length());
		if (!NOME.matcher(nome).matches()) {
			return;
		}
		try {
			Files.deleteIfExists(diretorio.resolve(nome));
		} catch (IOException ex) {
			LOG.warn("Não foi possível apagar a foto {}: {}", nome, ex.getMessage());
		}
	}

	private byte[] lerBytes(MultipartFile foto) {
		try {
			return foto.getBytes();
		} catch (IOException ex) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não foi possível ler a foto.");
		}
	}

	private String extensao(byte[] conteudo) {
		if (FormatoFoto.jpeg(conteudo)) {
			return ".jpg";
		}
		if (FormatoFoto.png(conteudo)) {
			return ".png";
		}
		throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Envie uma foto JPG ou PNG.");
	}

	private String nomeArquivo(String original, String extensao) {
		String base = "foto";
		if (original != null && !original.isBlank()) {
			String normalizado = original.replace('\\', '/');
			int barra = normalizado.lastIndexOf('/');
			String soNome = (barra >= 0 ? normalizado.substring(barra + 1) : normalizado).trim();
			if (!soNome.isBlank() && !soNome.equals(".") && !soNome.equals("..")) {
				base = soNome;
			}
		}
		if (base.toLowerCase(Locale.ROOT).endsWith(extensao)) {
			return base;
		}
		int ponto = base.lastIndexOf('.');
		if (ponto > 0) {
			base = base.substring(0, ponto);
		}
		return base + extensao;
	}

	private String limiteLegivel() {
		long mega = 1024L * 1024L;
		if (tamanhoMaximo % mega == 0) {
			return (tamanhoMaximo / mega) + " MB";
		}
		return tamanhoMaximo + " bytes";
	}

	public record FotoRecebida(byte[] conteudo, String nomeArquivo) {
	}

	public record FotoGuardada(String nome, Resource recurso, MediaType tipo) {
	}

}
