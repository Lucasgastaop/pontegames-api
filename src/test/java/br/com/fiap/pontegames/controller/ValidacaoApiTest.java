package br.com.fiap.pontegames.controller;

import br.com.fiap.pontegames.model.TipoUsuario;
import br.com.fiap.pontegames.model.Usuario;
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

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ValidacaoApiTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private String tokenAdmin;

	@BeforeEach
	void prepararAdmin() throws Exception {
		usuarioRepository.findByEmail("admin.validacao@pontegames.test").orElseGet(() -> {
			Usuario admin = new Usuario();
			admin.setNome("Admin Validacao");
			admin.setEmail("admin.validacao@pontegames.test");
			admin.setCpf("999.999.008-98");
			admin.setTipo(TipoUsuario.ADMIN);
			admin.setSenha(passwordEncoder.encode("senha123"));
			return usuarioRepository.saveAndFlush(admin);
		});
		MvcResult resultado = mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"admin.validacao@pontegames.test","senha":"senha123"}
								"""))
				.andExpect(status().isOk())
				.andReturn();
		String corpo = resultado.getResponse().getContentAsString();
		int inicio = corpo.indexOf("\"token\":\"") + 9;
		tokenAdmin = corpo.substring(inicio, corpo.indexOf('"', inicio));
	}

	@Test
	void usuarioSemNomeRetornaCamposInvalidos() throws Exception {
		comoAdmin(post("/api/usuarios")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"ana@pontegames.test","cpf":"000.000.001-01"}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.erro").value("Requisição inválida"))
				.andExpect(jsonPath("$.mensagem").value("Revise os campos informados."))
				.andExpect(jsonPath("$.campos[*].campo", hasItem("nome")))
				.andExpect(jsonPath("$.campos[?(@.campo == 'nome')].mensagem", hasItem("Nome é obrigatório.")));
	}

	@Test
	void cpfForaDoFormatoRetorna400() throws Exception {
		comoAdmin(post("/api/usuarios")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"nome":"Ana Souza","email":"ana@pontegames.test","cpf":"00000000101"}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.campos[*].campo", hasItem("cpf")));
	}

	@Test
	void pesoZeroNaoCriaDescarte() throws Exception {
		comoAdmin(post("/api/descartes")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"usuarioId":1,"pontoId":1,"tipoResiduoId":1,"pesoKg":0}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.campos[?(@.campo == 'pesoKg')].mensagem", hasItem("Peso deve ser maior que zero.")));
	}

	@Test
	void jsonInvalidoRetorna400() throws Exception {
		comoAdmin(post("/api/usuarios")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensagem").value("Corpo da requisição inválido."))
				.andExpect(jsonPath("$.campos").isEmpty());
	}

	@Test
	void usuarioInexistenteUsaOMesmoFormato() throws Exception {
		comoAdmin(get("/api/usuarios/999"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.erro").value("Não encontrado"))
				.andExpect(jsonPath("$.mensagem").value("Usuário não encontrado."))
				.andExpect(jsonPath("$.campos").isEmpty());
	}

	@Test
	void identificadorQueNaoENumeroRetorna400() throws Exception {
		comoAdmin(get("/api/usuarios/abc"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensagem").value("Valor inválido para id."));
	}

	private ResultActions comoAdmin(MockHttpServletRequestBuilder builder) throws Exception {
		return mockMvc.perform(builder.header("Authorization", "Bearer " + tokenAdmin));
	}

}
