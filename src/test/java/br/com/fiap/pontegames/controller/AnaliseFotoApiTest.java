package br.com.fiap.pontegames.controller;

import br.com.fiap.pontegames.model.TipoUsuario;
import br.com.fiap.pontegames.model.Usuario;
import br.com.fiap.pontegames.repository.DescarteRepository;
import br.com.fiap.pontegames.repository.NotificacaoRepository;
import br.com.fiap.pontegames.repository.PontoColetaRepository;
import br.com.fiap.pontegames.repository.TipoResiduoRepository;
import br.com.fiap.pontegames.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AnaliseFotoApiTest {

	private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xD9};
	private static final byte[] PNG = {
			(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
	};

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private NotificacaoRepository notificacaoRepository;

	@Autowired
	private DescarteRepository descarteRepository;

	@Autowired
	private PontoColetaRepository pontoColetaRepository;

	@Autowired
	private TipoResiduoRepository tipoResiduoRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private long cidadaoId;
	private String tokenCidadao;
	private long tipoId;
	private long pontoId;

	@BeforeEach
	void preparar() throws Exception {
		notificacaoRepository.deleteAll();
		descarteRepository.deleteAll();
		pontoColetaRepository.deleteAll();
		tipoResiduoRepository.deleteAll();
		usuarioRepository.deleteAll();
		cidadaoId = salvar("Ana Souza", "ana.foto@pontegames.test", "999.999.021-21");
		tokenCidadao = login("ana.foto@pontegames.test");
		String tokenGestor = loginDeGestor();
		tipoId = criar(tokenGestor, "/api/tipos-residuo", """
				{"nome":"Óleo de Cozinha","instrucao":"Garrafa PET fechada.","xpPorKg":80.00}
				""");
		pontoId = criar(tokenGestor, "/api/pontos-coleta", """
				{"nome":"Ecoponto Vila Mariana","endereco":"Rua Domingos de Morais, 1000","latitude":-23.5881,"longitude":-46.6342}
				""");
	}

	@Test
	void fotoCompativelCreditaXpNaHora() throws Exception {
		long descarteId = criar(tokenCidadao, "/api/descartes", descarte("1.00", "https://storage.pontegames.test/oleo.jpg"));

		mockMvc.perform(get("/api/descartes/" + descarteId).header("Authorization", "Bearer " + tokenCidadao))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("VALIDADO_IA"))
				.andExpect(jsonPath("$.parecer").value("COMPATIVEL"))
				.andExpect(jsonPath("$.mensagemParecer").value("A foto parece compatível com Óleo de Cozinha."))
				.andExpect(jsonPath("$.xpGanho").value(80));
		mockMvc.perform(get("/api/usuarios/me").header("Authorization", "Bearer " + tokenCidadao))
				.andExpect(jsonPath("$.xpTotal").value(80));
	}

	@Test
	void fotoIncompativelFalhaESemFoto() throws Exception {
		long incompativel = criar(tokenCidadao, "/api/descartes",
				descarte("1.00", "https://storage.pontegames.test/incompativel.jpg"));
		mockMvc.perform(get("/api/descartes/" + incompativel).header("Authorization", "Bearer " + tokenCidadao))
				.andExpect(jsonPath("$.parecer").value("INCOMPATIVEL"))
				.andExpect(jsonPath("$.mensagemParecer").value("A foto não parece ser Óleo de Cozinha."))
				.andExpect(jsonPath("$.status").value("REJEITADO"));

		long falha = criar(tokenCidadao, "/api/descartes",
				descarte("1.50", "https://storage.pontegames.test/falha.jpg"));
		mockMvc.perform(get("/api/descartes/" + falha).header("Authorization", "Bearer " + tokenCidadao))
				.andExpect(jsonPath("$.parecer").value("INCONCLUSIVO"))
				.andExpect(jsonPath("$.mensagemParecer").value("Não foi possível analisar a foto agora. Envie outra imagem."))
				.andExpect(jsonPath("$.status").value("PENDENTE"));

		long semFoto = criar(tokenCidadao, "/api/descartes", descarte("2.00", null));
		mockMvc.perform(get("/api/descartes/" + semFoto).header("Authorization", "Bearer " + tokenCidadao))
				.andExpect(jsonPath("$.parecer").value(nullValue()))
				.andExpect(jsonPath("$.mensagemParecer").value(nullValue()));
	}

	@Test
	void atualizarFotoTrocaOParecer() throws Exception {
		long descarteId = criar(tokenCidadao, "/api/descartes", descarte("1.00", "https://storage.pontegames.test/falha.jpg"));

		mockMvc.perform(put("/api/descartes/" + descarteId)
						.header("Authorization", "Bearer " + tokenCidadao)
						.contentType(MediaType.APPLICATION_JSON)
						.content(descarte("1.00", "https://storage.pontegames.test/incompativel.jpg")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.parecer").value("INCOMPATIVEL"))
				.andExpect(jsonPath("$.status").value("REJEITADO"));
	}

	@Test
	void arquivoJpgViraFotoDaApi() throws Exception {
		MvcResult criado = mockMvc.perform(formulario(
						"POST", "/api/descartes", "1.10", "oleo.jpg", MediaType.IMAGE_JPEG_VALUE, JPEG))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("VALIDADO_IA"))
				.andExpect(jsonPath("$.parecer").value("COMPATIVEL"))
				.andExpect(jsonPath("$.fotoUrl", containsString("/api/fotos/")))
				.andExpect(jsonPath("$.fotoUrl", endsWith(".jpg")))
				.andReturn();
		String fotoUrl = valorJson(criado.getResponse().getContentAsString(), "fotoUrl");

		mockMvc.perform(get(fotoUrl).header("Authorization", "Bearer " + tokenCidadao))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.IMAGE_JPEG))
				.andExpect(content().bytes(JPEG));
		mockMvc.perform(get(fotoUrl))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/fotos/nao-existe.jpg").header("Authorization", "Bearer " + tokenCidadao))
				.andExpect(status().isNotFound());
	}

	@Test
	void arquivoPngIncompativelETipoRecusado() throws Exception {
		MvcResult criado = mockMvc.perform(formulario(
						"POST", "/api/descartes", "1.20", "incompativel.png", MediaType.IMAGE_PNG_VALUE, PNG))
				.andExpect(status().isCreated())
				.andReturn();
		String fotoUrl = valorJson(criado.getResponse().getContentAsString(), "fotoUrl");

		mockMvc.perform(get("/api/descartes/" + idJson(criado.getResponse().getContentAsString()))
						.header("Authorization", "Bearer " + tokenCidadao))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.parecer").value("INCOMPATIVEL"))
				.andExpect(jsonPath("$.status").value("REJEITADO"))
				.andExpect(jsonPath("$.fotoUrl", endsWith(".png")));
		mockMvc.perform(get(fotoUrl).header("Authorization", "Bearer " + tokenCidadao))
				.andExpect(content().contentType(MediaType.IMAGE_PNG))
				.andExpect(content().bytes(PNG));

		mockMvc.perform(formulario("POST", "/api/descartes", "1.30", "nota.gif", "image/gif", "GIF89a".getBytes()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensagem").value("Envie uma foto JPG ou PNG."));
	}

	@Test
	void arquivoGrandeDemais() throws Exception {
		byte[] grande = new byte[600];
		grande[0] = (byte) 0xFF;
		grande[1] = (byte) 0xD8;
		grande[2] = (byte) 0xFF;

		mockMvc.perform(formulario("POST", "/api/descartes", "1.40", "oleo.jpg", MediaType.IMAGE_JPEG_VALUE, grande))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensagem", containsString("máximo")));
	}

	@Test
	void multipartSemArquivoNaoGeraParecer() throws Exception {
		mockMvc.perform(formulario("POST", "/api/descartes", "1.60", null, null, null))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.parecer").value(nullValue()))
				.andExpect(jsonPath("$.fotoUrl").value(nullValue()));
	}

	@Test
	void atualizarComArquivoTrocaOParecerEApagaAFotoAntiga() throws Exception {
		MvcResult criado = mockMvc.perform(formulario(
						"POST", "/api/descartes", "1.70", "falha.jpg", MediaType.IMAGE_JPEG_VALUE, JPEG))
				.andExpect(status().isCreated())
				.andReturn();
		String corpo = criado.getResponse().getContentAsString();
		String antiga = valorJson(corpo, "fotoUrl");
		long id = idJson(corpo);

		MvcResult atualizado = mockMvc.perform(formulario(
						"PUT", "/api/descartes/" + id, "1.70", "incompativel.png", MediaType.IMAGE_PNG_VALUE, PNG))
				.andExpect(status().isOk())
				.andReturn();
		String nova = valorJson(atualizado.getResponse().getContentAsString(), "fotoUrl");

		mockMvc.perform(get("/api/descartes/" + id).header("Authorization", "Bearer " + tokenCidadao))
				.andExpect(jsonPath("$.parecer").value("INCOMPATIVEL"))
				.andExpect(jsonPath("$.status").value("REJEITADO"));
		mockMvc.perform(get(antiga).header("Authorization", "Bearer " + tokenCidadao))
				.andExpect(status().isNotFound());
		mockMvc.perform(get(nova).header("Authorization", "Bearer " + tokenCidadao))
				.andExpect(status().isOk());

		mockMvc.perform(delete("/api/descartes/" + id).header("Authorization", "Bearer " + tokenCidadao))
				.andExpect(status().isNoContent());
		mockMvc.perform(get(nova).header("Authorization", "Bearer " + tokenCidadao))
				.andExpect(status().isNotFound());
	}

	private String loginDeGestor() throws Exception {
		salvarGestor();
		return login("elena.foto@pontegames.test");
	}

	private void salvarGestor() {
		Usuario usuario = new Usuario();
		usuario.setNome("Elena Duarte");
		usuario.setEmail("elena.foto@pontegames.test");
		usuario.setCpf("999.999.022-22");
		usuario.setTipo(TipoUsuario.GESTOR);
		usuario.setSenha(passwordEncoder.encode("senha123"));
		usuarioRepository.saveAndFlush(usuario);
	}

	private String descarte(String peso, String foto) {
		String fotoJson = foto == null ? "" : ",\"fotoUrl\":\"" + foto + "\"";
		return """
				{"usuarioId":%d,"pontoId":%d,"tipoResiduoId":%d,"pesoKg":%s%s}
				""".formatted(cidadaoId, pontoId, tipoId, peso, fotoJson);
	}

	private long salvar(String nome, String email, String cpf) {
		Usuario usuario = new Usuario();
		usuario.setNome(nome);
		usuario.setEmail(email);
		usuario.setCpf(cpf);
		usuario.setTipo(TipoUsuario.CIDADAO);
		usuario.setSenha(passwordEncoder.encode("senha123"));
		return usuarioRepository.saveAndFlush(usuario).getId();
	}

	private String login(String email) throws Exception {
		MvcResult resultado = mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","senha":"senha123"}
								""".formatted(email)))
				.andExpect(status().isOk())
				.andReturn();
		String corpo = resultado.getResponse().getContentAsString();
		int inicio = corpo.indexOf("\"token\":\"") + 9;
		return corpo.substring(inicio, corpo.indexOf('"', inicio));
	}

	private long criar(String token, String url, String json) throws Exception {
		MvcResult resultado = mockMvc.perform(post(url)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(json))
				.andExpect(status().isCreated())
				.andReturn();
		String corpo = resultado.getResponse().getContentAsString();
		int inicio = corpo.indexOf("\"id\":") + 5;
		int fim = inicio;
		while (fim < corpo.length() && Character.isDigit(corpo.charAt(fim))) {
			fim++;
		}
		return Long.parseLong(corpo.substring(inicio, fim));
	}

	private MockMultipartHttpServletRequestBuilder formulario(
			String metodo,
			String url,
			String peso,
			String nome,
			String tipo,
			byte[] bytes
	) {
		MockMultipartHttpServletRequestBuilder pedido = multipart(url);
		pedido.param("usuarioId", String.valueOf(cidadaoId));
		pedido.param("pontoId", String.valueOf(pontoId));
		pedido.param("tipoResiduoId", String.valueOf(tipoId));
		pedido.param("pesoKg", peso);
		pedido.header("Authorization", "Bearer " + tokenCidadao);
		if (bytes != null) {
			pedido.file(new MockMultipartFile("foto", nome, tipo, bytes));
		}
		if ("PUT".equals(metodo)) {
			pedido.with(request -> {
				request.setMethod("PUT");
				return request;
			});
		}
		return pedido;
	}

	private String valorJson(String corpo, String campo) {
		String chave = "\"" + campo + "\":\"";
		int inicio = corpo.indexOf(chave);
		if (inicio < 0) {
			throw new IllegalStateException("Campo ausente: " + campo);
		}
		inicio += chave.length();
		return corpo.substring(inicio, corpo.indexOf('"', inicio));
	}

	private long idJson(String corpo) {
		int inicio = corpo.indexOf("\"id\":") + 5;
		int fim = inicio;
		while (fim < corpo.length() && Character.isDigit(corpo.charAt(fim))) {
			fim++;
		}
		return Long.parseLong(corpo.substring(inicio, fim));
	}

}
