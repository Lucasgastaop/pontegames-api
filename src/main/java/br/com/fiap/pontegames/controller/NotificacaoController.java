package br.com.fiap.pontegames.controller;

import br.com.fiap.pontegames.dto.NotificacaoResponse;
import br.com.fiap.pontegames.dto.PaginaResponse;
import br.com.fiap.pontegames.service.NotificacaoService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notificacoes")
public class NotificacaoController {

	private final NotificacaoService notificacaoService;

	public NotificacaoController(NotificacaoService notificacaoService) {
		this.notificacaoService = notificacaoService;
	}

	@GetMapping
	public PaginaResponse<NotificacaoResponse> listar(
			@PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
	) {
		return notificacaoService.listar(pageable);
	}

}
