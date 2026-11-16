package br.com.fiap.pontegames.controller;

import br.com.fiap.pontegames.dto.StatusResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/status")
public class StatusController {

	@GetMapping
	public StatusResponse status() {
		return new StatusResponse("pontegames-api", "ok");
	}

}
