package br.com.fiap.pontegames.controller;

import br.com.fiap.pontegames.dto.StatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/status")
@Tag(name = "Status", description = "Confere se a API subiu.")
public class StatusController {

	@GetMapping
	@Operation(summary = "Retorna o nome da aplicação e o status ok.")
	public StatusResponse status() {
		return new StatusResponse("pontegames-api", "ok");
	}

}
