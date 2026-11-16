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

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificacaoApiTest {

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
		cidadaoId = salvar(TipoUsuario.CIDADAO, "Ana Souza", "ana.aviso@pontegames.test", "999.999.031-31");
		salvar(TipoUsuario.CIDADAO, "Bruno Lima", "bruno.aviso@pontegames.test", "999.999.032-32");
		salvar(TipoUsuario.GESTOR, "Elena Duarte", "elena.aviso@pontegames.test", "999.999.033-33");
		tokenCidadao = login("ana.aviso@pontegames.test");
		tokenGestor = login("elena.aviso@pontegames.test");
		tipoId = criar(tokenGestor, "/api/tipos-residuo", """
				{"nome":"Óleo de Cozinha","instrucao":"Garrafa PET fechada.","xpPorKg":80.00}
				""");
		pontoId = criar(tokenGestor, "/api/pontos-coleta", """
				{"nome":"Ecoponto Vila Mariana","endereco":"Rua Domingos de Morais, 1000","latitude":-23.5881,"longitude":-46.6342}
				""");
	}

	@Test
	void validacaoGeraAvisoSoParaOCidadao() throws Exception {
		long validado = registrar("2.00");
		validar(validado, "VALIDADO_IA");
		long rejeitado = registrar("1.00");
		validar(rejeitado, "REJEITADO");

		mockMvc.perform(get("/api/notificacoes").header("Authorization", "Bearer " + tokenCidadao))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElementos").value(2))
				.andExpect(jsonPath("$.conteudo[*].mensagem").value(hasItem(
						"Seu descarte de Óleo de Cozinha foi validado. Você ganhou 160 XP e está no nível 1.")))
				.andExpect(jsonPath("$.conteudo[*].mensagem").value(hasItem(
						"Seu descarte de Óleo de Cozinha foi rejeitado. Nenhum XP foi creditado.")))
				.andExpect(jsonPath("$.conteudo[0].dataCriacao").isNotEmpty());

		String tokenBruno = login("bruno.aviso@pontegames.test");
		mockMvc.perform(get("/api/notificacoes").header("Authorization", "Bearer " + tokenBruno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElementos").value(0));
		mockMvc.perform(get("/api/notificacoes").header("Authorization", "Bearer " + tokenGestor))
				.andExpect(jsonPath("$.totalElementos").value(0));
	}

	@Test
	void listarSemTokenRecusa() throws Exception {
		mockMvc.perform(get("/api/notificacoes"))
				.andExpect(status().isUnauthorized());
	}

	private void validar(long descarteId, String status) throws Exception {
		mockMvc.perform(post("/api/descartes/" + descarteId + "/validacao")
						.header("Authorization", "Bearer " + tokenGestor)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"status":"%s"}
								""".formatted(status)))
				.andExpect(status().isOk());
	}

	private long registrar(String peso) throws Exception {
		return criar(tokenCidadao, "/api/descartes", """
				{"usuarioId":%d,"pontoId":%d,"tipoResiduoId":%d,"pesoKg":%s}
				""".formatted(cidadaoId, pontoId, tipoId, peso));
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
