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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RegrasDescarteApiTest {

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
	private String tokenGestor;
	private long tipoId;
	private long pontoId;

	@BeforeEach
	void preparar() throws Exception {
		notificacaoRepository.deleteAll();
		descarteRepository.deleteAll();
		pontoColetaRepository.deleteAll();
		tipoResiduoRepository.deleteAll();
		usuarioRepository.deleteAll();
		cidadaoId = salvar(TipoUsuario.CIDADAO, "Ana Souza", "ana.regras@pontegames.test", "999.999.011-11");
		salvar(TipoUsuario.GESTOR, "Elena Duarte", "elena.regras@pontegames.test", "999.999.012-12");
		tokenCidadao = login("ana.regras@pontegames.test");
		tokenGestor = login("elena.regras@pontegames.test");
		tipoId = criar(tokenGestor, "/api/tipos-residuo", """
				{"nome":"Óleo de Cozinha","instrucao":"Garrafa PET fechada.","xpPorKg":80.00}
				""");
		pontoId = criar(tokenGestor, "/api/pontos-coleta", """
				{"nome":"Ecoponto Vila Mariana","endereco":"Rua Domingos de Morais, 1000","latitude":-23.5881,"longitude":-46.6342}
				""");
	}

	@Test
	void validacaoCreditaXpESobeNivel() throws Exception {
		long descarteId = registrar(cidadaoId, "2.00");

		mockMvc.perform(get("/api/descartes/" + descarteId).header("Authorization", "Bearer " + tokenCidadao))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("PENDENTE"))
				.andExpect(jsonPath("$.xpGanho").value(0));
		mockMvc.perform(get("/api/usuarios/me").header("Authorization", "Bearer " + tokenCidadao))
				.andExpect(jsonPath("$.xpTotal").value(0))
				.andExpect(jsonPath("$.nivel").value(1));

		mockMvc.perform(post("/api/descartes/" + descarteId + "/validacao")
						.header("Authorization", "Bearer " + tokenGestor)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"status":"VALIDADO_IA"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("VALIDADO_IA"));

		mockMvc.perform(get("/api/usuarios/me").header("Authorization", "Bearer " + tokenCidadao))
				.andExpect(jsonPath("$.xpTotal").value(160))
				.andExpect(jsonPath("$.nivel").value(1));

		long segundo = registrar(cidadaoId, "1.00");
		validar(segundo);
		mockMvc.perform(get("/api/usuarios/me").header("Authorization", "Bearer " + tokenCidadao))
				.andExpect(jsonPath("$.xpTotal").value(240))
				.andExpect(jsonPath("$.nivel").value(2));
	}

	@Test
	void cidadaoNaoValidaDescarte() throws Exception {
		long descarteId = registrar(cidadaoId, "1.00");
		mockMvc.perform(post("/api/descartes/" + descarteId + "/validacao")
						.header("Authorization", "Bearer " + tokenCidadao)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"status":"VALIDADO_IA"}
								"""))
				.andExpect(status().isForbidden());
	}

	@Test
	void ecopontoInativoDuplicadoELimiteDiario() throws Exception {
		long inativo = criar(tokenGestor, "/api/pontos-coleta", """
				{"nome":"Ecoponto Centro","endereco":"Praça da Sé","latitude":-23.5505,"longitude":-46.6333,"ativo":false}
				""");
		mockMvc.perform(post("/api/descartes")
						.header("Authorization", "Bearer " + tokenCidadao)
						.contentType(MediaType.APPLICATION_JSON)
						.content(descarteJson(cidadaoId, inativo, "1.00")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.mensagem").value("Ecoponto inativo não recebe descarte."));

		registrar(cidadaoId, "1.00");
		mockMvc.perform(post("/api/descartes")
						.header("Authorization", "Bearer " + tokenCidadao)
						.contentType(MediaType.APPLICATION_JSON)
						.content(descarteJson(cidadaoId, pontoId, "1.00")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.mensagem").value("Descarte duplicado no mesmo dia para o mesmo cidadão, ecoponto, tipo e peso."));

		registrar(cidadaoId, "4.00");
		mockMvc.perform(post("/api/descartes")
						.header("Authorization", "Bearer " + tokenCidadao)
						.contentType(MediaType.APPLICATION_JSON)
						.content(descarteJson(cidadaoId, pontoId, "2.00")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.mensagem").value("Limite diário excedido para este resíduo. Limite: 5 kg."));
	}

	@Test
	void rejeitadoPermiteNovaTentativaEExclusaoEstornaXp() throws Exception {
		long rejeitado = registrar(cidadaoId, "1.00");
		mockMvc.perform(post("/api/descartes/" + rejeitado + "/validacao")
						.header("Authorization", "Bearer " + tokenGestor)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"status":"REJEITADO"}
								"""))
				.andExpect(status().isOk());
		registrar(cidadaoId, "1.00");

		long validado = registrar(cidadaoId, "2.00");
		validar(validado);
		mockMvc.perform(delete("/api/descartes/" + validado).header("Authorization", "Bearer " + tokenGestor))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/usuarios/me").header("Authorization", "Bearer " + tokenCidadao))
				.andExpect(jsonPath("$.xpTotal").value(0))
				.andExpect(jsonPath("$.nivel").value(1));
	}

	private void validar(long descarteId) throws Exception {
		mockMvc.perform(post("/api/descartes/" + descarteId + "/validacao")
						.header("Authorization", "Bearer " + tokenGestor)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"status":"VALIDADO_IA"}
								"""))
				.andExpect(status().isOk());
	}

	private long registrar(long usuarioId, String peso) throws Exception {
		return criar(tokenCidadao, "/api/descartes", descarteJson(usuarioId, pontoId, peso));
	}

	private String descarteJson(long usuarioId, long ponto, String peso) {
		return """
				{"usuarioId":%d,"pontoId":%d,"tipoResiduoId":%d,"pesoKg":%s}
				""".formatted(usuarioId, ponto, tipoId, peso);
	}

	private long salvar(TipoUsuario tipo, String nome, String email, String cpf) {
		Usuario usuario = new Usuario();
		usuario.setNome(nome);
		usuario.setEmail(email);
		usuario.setCpf(cpf);
		usuario.setTipo(tipo);
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
