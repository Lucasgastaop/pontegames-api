package br.com.fiap.pontegames.controller;

import br.com.fiap.pontegames.dto.PaginaResponse;
import br.com.fiap.pontegames.dto.TipoResiduoRequest;
import br.com.fiap.pontegames.dto.TipoResiduoResponse;
import br.com.fiap.pontegames.service.TipoResiduoService;
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
@RequestMapping("/api/tipos-residuo")
@Tag(name = "Tipos de resíduo", description = "Óleo, eletrônico e pilhas, com XP por quilo.")
public class TipoResiduoController {

	private final TipoResiduoService tipoResiduoService;

	public TipoResiduoController(TipoResiduoService tipoResiduoService) {
		this.tipoResiduoService = tipoResiduoService;
	}

	@GetMapping
	public PaginaResponse<TipoResiduoResponse> listar(@PageableDefault(size = 10, sort = "nome") Pageable pageable) {
		return tipoResiduoService.listar(pageable);
	}

	@GetMapping("/{id}")
	public TipoResiduoResponse buscar(@PathVariable Long id) {
		return tipoResiduoService.buscar(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public TipoResiduoResponse criar(@Valid @RequestBody TipoResiduoRequest request) {
		return tipoResiduoService.criar(request);
	}

	@PutMapping("/{id}")
	public TipoResiduoResponse atualizar(@PathVariable Long id, @Valid @RequestBody TipoResiduoRequest request) {
		return tipoResiduoService.atualizar(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void excluir(@PathVariable Long id) {
		tipoResiduoService.excluir(id);
	}

}
