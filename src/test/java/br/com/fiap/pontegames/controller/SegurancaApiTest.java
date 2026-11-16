package br.com.fiap.pontegames.controller;

import br.com.fiap.pontegames.model.TipoUsuario;
import br.com.fiap.pontegames.model.Usuario;
import br.com.fiap.pontegames.repository.DescarteRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SegurancaApiTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private DescarteRepository descarteRepository;

	@Autowired
	private PontoColetaRepository pontoColetaRepository;

	@Autowired
	private TipoResiduoRepository tipoResiduoRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private String tokenCidadao;
	private long idCidadao;

	@BeforeEach
	void limpar() throws Exception {
		descarteRepository.deleteAll();
		pontoColetaRepository.deleteAll();
		tipoResiduoRepository.deleteAll();
		usuarioRepository.deleteAll();
		idCidadao = salvar(TipoUsuario.CIDADAO, "Ana Souza", "ana.seguranca@pontegames.test", "999.999.002-92");
		tokenCidadao = login("ana.seguranca@pontegames.test");
	}

	@Test
	void cadastroPublicoNasceCidadao() throws Exception {
		mockMvc.perform(post("/api/auth/registrar")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"nome":"Novo Cidadao","email":"novo@pontegames.test","cpf":"000.000.010-10","senha":"senha123","tipo":"ADMIN"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.tipo").value("CIDADAO"))
				.andExpect(jsonPath("$.senha").doesNotExist());
	}

	@Test
	void loginRecusaSenhaErrada() throws Exception {
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"ana.seguranca@pontegames.test","senha":"errada1"}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.erro").value("Não autenticado"))
				.andExpect(jsonPath("$.mensagem").value("E-mail ou senha inválidos."));
	}

	@Test
	void rotaProtegidaSemTokenRetorna401() throws Exception {
		mockMvc.perform(get("/api/pontos-coleta"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.mensagem").value("Autenticação necessária."));
	}

	@Test
	void cidadaoNaoCadastraEcopontoNemDescarteDeOutro() throws Exception {
		long adminId = salvar(TipoUsuario.ADMIN, "Fabio Nunes", "fabio.seguranca@pontegames.test", "999.999.003-93");
		String tokenAdmin = login("fabio.seguranca@pontegames.test");
		long tipoId = criar(tokenAdmin, "/api/tipos-residuo", """
				{"nome":"Oleo de Cozinha","instrucao":"Garrafa PET fechada.","xpPorKg":80.00}
				""");
		long pontoId = criar(tokenAdmin, "/api/pontos-coleta", """
				{"nome":"Ecoponto Pinheiros","endereco":"Rua dos Pinheiros, 100","latitude":-23.567,"longitude":-46.692}
				""");

		mockMvc.perform(get("/api/usuarios/me").header("Authorization", "Bearer " + tokenCidadao))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(idCidadao))
				.andExpect(jsonPath("$.senha").doesNotExist());

		mockMvc.perform(post("/api/pontos-coleta")
						.header("Authorization", "Bearer " + tokenCidadao)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"nome":"Ponto clandestino","endereco":"Rua X","latitude":0,"longitude":0}
								"""))
				.andExpect(status().isForbidden());

		mockMvc.perform(post("/api/descartes")
						.header("Authorization", "Bearer " + tokenCidadao)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"usuarioId":%d,"pontoId":%d,"tipoResiduoId":%d,"pesoKg":1.00}
								""".formatted(adminId, pontoId, tipoId)))
				.andExpect(status().isForbidden());

		mockMvc.perform(post("/api/descartes")
						.header("Authorization", "Bearer " + tokenCidadao)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"usuarioId":%d,"pontoId":%d,"tipoResiduoId":%d,"pesoKg":1.00}
								""".formatted(idCidadao, pontoId, tipoId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("PENDENTE"));
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
				.andExpect(jsonPath("$.tipoToken").value("Bearer"))
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
