package br.com.fiap.pontegames.controller;

import br.com.fiap.pontegames.dto.PaginaResponse;
import br.com.fiap.pontegames.dto.PontoColetaRequest;
import br.com.fiap.pontegames.dto.PontoColetaResponse;
import br.com.fiap.pontegames.service.PontoColetaService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pontos-coleta")
public class PontoColetaController {

	private final PontoColetaService pontoColetaService;

	public PontoColetaController(PontoColetaService pontoColetaService) {
		this.pontoColetaService = pontoColetaService;
	}

	@GetMapping
	public PaginaResponse<PontoColetaResponse> listar(@PageableDefault(size = 10, sort = "nome") Pageable pageable) {
		return pontoColetaService.listar(pageable);
	}

	@GetMapping("/{id}")
	public PontoColetaResponse buscar(@PathVariable Long id) {
		return pontoColetaService.buscar(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public PontoColetaResponse criar(@Valid @RequestBody PontoColetaRequest request) {
		return pontoColetaService.criar(request);
	}

	@PutMapping("/{id}")
	public PontoColetaResponse atualizar(@PathVariable Long id, @Valid @RequestBody PontoColetaRequest request) {
		return pontoColetaService.atualizar(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void excluir(@PathVariable Long id) {
		pontoColetaService.excluir(id);
	}

}
