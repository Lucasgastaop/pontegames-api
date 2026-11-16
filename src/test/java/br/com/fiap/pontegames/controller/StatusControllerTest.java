package br.com.fiap.pontegames.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StatusControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void documentacaoAbertaSemToken() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.info.title").value("Ponte Games API"))
				.andExpect(jsonPath("$.paths['/api/auth/login']").exists())
				.andExpect(jsonPath("$.paths['/api/descartes/{id}/validacao']").exists())
				.andExpect(jsonPath("$.paths['/api/notificacoes']").exists());
		mockMvc.perform(get("/swagger-ui/index.html"))
				.andExpect(status().isOk());
	}

	@Test
	void statusRetornaOk() throws Exception {
		mockMvc.perform(get("/api/status"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.aplicacao").value("pontegames-api"))
				.andExpect(jsonPath("$.status").value("ok"));
	}

}
