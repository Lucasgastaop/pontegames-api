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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AnaliseFotoApiTest {

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
	void fotoCompativelNaoCreditaXp() throws Exception {
		long descarteId = criar(tokenCidadao, "/api/descartes", descarte("1.00", "https://storage.pontegames.test/oleo.jpg"));

		mockMvc.perform(get("/api/descartes/" + descarteId).header("Authorization", "Bearer " + tokenCidadao))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("PENDENTE"))
				.andExpect(jsonPath("$.parecer").value("COMPATIVEL"))
				.andExpect(jsonPath("$.mensagemParecer").value("A foto parece compatível com Óleo de Cozinha."))
				.andExpect(jsonPath("$.xpGanho").value(80));
		mockMvc.perform(get("/api/usuarios/me").header("Authorization", "Bearer " + tokenCidadao))
				.andExpect(jsonPath("$.xpTotal").value(0));
	}

	@Test
	void fotoIncompativelFalhaESemFoto() throws Exception {
		long incompativel = criar(tokenCidadao, "/api/descartes",
				descarte("1.00", "https://storage.pontegames.test/incompativel.jpg"));
		mockMvc.perform(get("/api/descartes/" + incompativel).header("Authorization", "Bearer " + tokenCidadao))
				.andExpect(jsonPath("$.parecer").value("INCOMPATIVEL"))
				.andExpect(jsonPath("$.mensagemParecer").value("A foto não parece ser Óleo de Cozinha."))
				.andExpect(jsonPath("$.status").value("PENDENTE"));

		long falha = criar(tokenCidadao, "/api/descartes",
				descarte("1.50", "https://storage.pontegames.test/falha.jpg"));
		mockMvc.perform(get("/api/descartes/" + falha).header("Authorization", "Bearer " + tokenCidadao))
				.andExpect(jsonPath("$.parecer").value("INCONCLUSIVO"))
				.andExpect(jsonPath("$.mensagemParecer").value("Não foi possível analisar a foto agora. O gestor confere na validação."));

		long semFoto = criar(tokenCidadao, "/api/descartes", descarte("2.00", null));
		mockMvc.perform(get("/api/descartes/" + semFoto).header("Authorization", "Bearer " + tokenCidadao))
				.andExpect(jsonPath("$.parecer").value(nullValue()))
				.andExpect(jsonPath("$.mensagemParecer").value(nullValue()));
	}

	@Test
	void atualizarFotoTrocaOParecer() throws Exception {
		long descarteId = criar(tokenCidadao, "/api/descartes", descarte("1.00", "https://storage.pontegames.test/oleo.jpg"));

		mockMvc.perform(put("/api/descartes/" + descarteId)
						.header("Authorization", "Bearer " + tokenCidadao)
						.contentType(MediaType.APPLICATION_JSON)
						.content(descarte("1.00", "https://storage.pontegames.test/incompativel.jpg")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.parecer").value("INCOMPATIVEL"))
				.andExpect(jsonPath("$.status").value("PENDENTE"));
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

}
