package br.com.fiap.pontegames.controller;

import br.com.fiap.pontegames.dto.DescarteRequest;
import br.com.fiap.pontegames.dto.DescarteResponse;
import br.com.fiap.pontegames.dto.PaginaResponse;
import br.com.fiap.pontegames.dto.ValidacaoDescarteRequest;
import br.com.fiap.pontegames.service.DescarteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/descartes")
@Tag(name = "Descartes", description = "Descarte do cidadão, parecer da foto e validação do gestor.")
public class DescarteController {

	private final DescarteService descarteService;

	public DescarteController(DescarteService descarteService) {
		this.descarteService = descarteService;
	}

	@GetMapping
	public PaginaResponse<DescarteResponse> listar(@PageableDefault(size = 10, sort = "id") Pageable pageable) {
		return descarteService.listar(pageable);
	}

	@GetMapping("/{id}")
	public DescarteResponse buscar(@PathVariable Long id) {
		return descarteService.buscar(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public DescarteResponse criar(@Valid @RequestBody DescarteRequest request) {
		return descarteService.criar(request);
	}

	@PutMapping("/{id}")
	public DescarteResponse atualizar(@PathVariable Long id, @Valid @RequestBody DescarteRequest request) {
		return descarteService.atualizar(id, request);
	}

	@PostMapping("/{id}/validacao")
	@Operation(summary = "Gestor ou admin valida ou rejeita um descarte pendente. VALIDADO_IA credita o XP.")
	public DescarteResponse validar(@PathVariable Long id, @Valid @RequestBody ValidacaoDescarteRequest request) {
		return descarteService.validar(id, request.status());
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void excluir(@PathVariable Long id) {
		descarteService.excluir(id);
	}

}
