package br.com.fiap.pontegames.controller;

import br.com.fiap.pontegames.dto.NotificacaoResponse;
import br.com.fiap.pontegames.dto.PaginaResponse;
import br.com.fiap.pontegames.service.NotificacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notificacoes")
@Tag(name = "Avisos", description = "Avisos gravados pelo consumidor do RabbitMQ para quem está logado.")
public class NotificacaoController {

	private final NotificacaoService notificacaoService;

	public NotificacaoController(NotificacaoService notificacaoService) {
		this.notificacaoService = notificacaoService;
	}

	@GetMapping
	@Operation(summary = "Lista os avisos do usuário autenticado, do mais recente para o mais antigo.")
	public PaginaResponse<NotificacaoResponse> listar(
			@PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
	) {
		return notificacaoService.listar(pageable);
	}

}
