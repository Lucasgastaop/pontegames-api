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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CrudApiTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private DescarteRepository descarteRepository;

	@Autowired
	private PontoColetaRepository pontoColetaRepository;

	@Autowired
	private TipoResiduoRepository tipoResiduoRepository;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private String tokenAdmin;

	@BeforeEach
	void limpar() throws Exception {
		descarteRepository.deleteAll();
		pontoColetaRepository.deleteAll();
		tipoResiduoRepository.deleteAll();
		usuarioRepository.deleteAll();
		tokenAdmin = tokenDe(TipoUsuario.ADMIN, "Zz Admin", "admin.teste@pontegames.test", "999.999.001-91");
	}

	@Test
	void criaConsultaAtualizaEExcluiDescartePendente() throws Exception {
		long usuarioId = criar("/api/usuarios", """
				{"nome":"Ana Souza","email":"ana.souza@pontegames.test","cpf":"000.000.001-01","senha":"senha123"}
				""");
		long tipoId = criar("/api/tipos-residuo", """
				{"nome":"Oleo de Cozinha","instrucao":"Garrafa PET fechada.","xpPorKg":80.00}
				""");
		long pontoId = criar("/api/pontos-coleta", """
				{"nome":"Ecoponto Vila Mariana","endereco":"Rua Domingos de Morais, 1000","latitude":-23.5881,"longitude":-46.6342}
				""");

		comoAdmin(get("/api/usuarios/" + usuarioId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tipo").value("CIDADAO"))
				.andExpect(jsonPath("$.nivel").value(1))
				.andExpect(jsonPath("$.xpTotal").value(0));

		comoAdmin(get("/api/pontos-coleta/" + pontoId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.ativo").value(true));

		String descarteJson = """
				{"usuarioId":%d,"pontoId":%d,"tipoResiduoId":%d,"pesoKg":2.00,"fotoUrl":"https://storage.pontegames.test/descartes/1.jpg"}
				""".formatted(usuarioId, pontoId, tipoId);
		long descarteId = criar("/api/descartes", descarteJson);

		comoAdmin(get("/api/descartes/" + descarteId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nomeUsuario").value("Ana Souza"))
				.andExpect(jsonPath("$.nomePonto").value("Ecoponto Vila Mariana"))
				.andExpect(jsonPath("$.nomeResiduo").value("Oleo de Cozinha"))
				.andExpect(jsonPath("$.status").value("PENDENTE"))
				.andExpect(jsonPath("$.xpGanho").value(0))
				.andExpect(jsonPath("$.dataDescarte").isNotEmpty());

		comoAdmin(put("/api/descartes/" + descarteId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"usuarioId":%d,"pontoId":%d,"tipoResiduoId":%d,"pesoKg":1.50,"fotoUrl":"https://storage.pontegames.test/descartes/1b.jpg","status":"VALIDADO_IA"}
								""".formatted(usuarioId, pontoId, tipoId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.pesoKg").value(1.5))
				.andExpect(jsonPath("$.status").value("PENDENTE"))
				.andExpect(jsonPath("$.xpGanho").value(0));

		comoAdmin(put("/api/usuarios/" + usuarioId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"nome":"Ana Souza Lima","email":"ana.souza@pontegames.test","cpf":"000.000.001-01","tipo":"GESTOR"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("Ana Souza Lima"))
				.andExpect(jsonPath("$.tipo").value("GESTOR"))
				.andExpect(jsonPath("$.nivel").value(1));

		comoAdmin(delete("/api/pontos-coleta/" + pontoId))
				.andExpect(status().isConflict());

		comoAdmin(delete("/api/descartes/" + descarteId))
				.andExpect(status().isNoContent());

		comoAdmin(get("/api/descartes/" + descarteId))
				.andExpect(status().isNotFound());
	}

	@Test
	void listaUsuariosEmPagina() throws Exception {
		criar("/api/usuarios", """
				{"nome":"Carla Mendes","email":"carla@pontegames.test","cpf":"000.000.003-03","senha":"senha123"}
				""");
		criar("/api/usuarios", """
				{"nome":"Ana Souza","email":"ana.pagina@pontegames.test","cpf":"000.000.001-01","senha":"senha123"}
				""");
		criar("/api/usuarios", """
				{"nome":"Bruno Lima","email":"bruno@pontegames.test","cpf":"000.000.002-02","senha":"senha123"}
				""");

		comoAdmin(get("/api/usuarios").param("size", "2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.pagina").value(0))
				.andExpect(jsonPath("$.tamanho").value(2))
				.andExpect(jsonPath("$.totalElementos").value(4))
				.andExpect(jsonPath("$.totalPaginas").value(2))
				.andExpect(jsonPath("$.conteudo.length()").value(2))
				.andExpect(jsonPath("$.conteudo[0].nome").value("Ana Souza"))
				.andExpect(jsonPath("$.conteudo[1].nome").value("Bruno Lima"));
	}

	@Test
	void usuarioInexistenteRetorna404() throws Exception {
		comoAdmin(get("/api/usuarios/999"))
				.andExpect(status().isNotFound());
	}

	@Test
	void emailDuplicadoRetorna409() throws Exception {
		criar("/api/usuarios", """
				{"nome":"Ana Souza","email":"ana.dup@pontegames.test","cpf":"000.000.001-01","senha":"senha123"}
				""");

		comoAdmin(post("/api/usuarios")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"nome":"Outra Ana","email":"ana.dup@pontegames.test","cpf":"000.000.009-09","senha":"senha123"}
								"""))
				.andExpect(status().isConflict());
	}

	private long criar(String url, String json) throws Exception {
		MvcResult resultado = comoAdmin(post(url)
						.contentType(MediaType.APPLICATION_JSON)
						.content(json))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNumber())
				.andReturn();
		String corpo = resultado.getResponse().getContentAsString();
		int inicio = corpo.indexOf("\"id\":") + 5;
		int fim = inicio;
		while (fim < corpo.length() && Character.isDigit(corpo.charAt(fim))) {
			fim++;
		}
		return Long.parseLong(corpo.substring(inicio, fim));
	}

	private ResultActions comoAdmin(MockHttpServletRequestBuilder builder) throws Exception {
		return mockMvc.perform(builder.header("Authorization", "Bearer " + tokenAdmin));
	}

	private String tokenDe(TipoUsuario tipo, String nome, String email, String cpf) throws Exception {
		Usuario usuario = new Usuario();
		usuario.setNome(nome);
		usuario.setEmail(email);
		usuario.setCpf(cpf);
		usuario.setTipo(tipo);
		usuario.setSenha(passwordEncoder.encode("senha123"));
		usuarioRepository.saveAndFlush(usuario);

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

}
