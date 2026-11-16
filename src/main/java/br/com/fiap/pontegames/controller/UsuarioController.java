package br.com.fiap.pontegames.controller;

import br.com.fiap.pontegames.dto.PaginaResponse;
import br.com.fiap.pontegames.dto.UsuarioAtualizacaoRequest;
import br.com.fiap.pontegames.dto.UsuarioRequest;
import br.com.fiap.pontegames.dto.UsuarioResponse;
import br.com.fiap.pontegames.service.UsuarioService;
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
@RequestMapping("/api/usuarios")
@Tag(name = "Usuários", description = "Perfil do cidadão e cadastro administrativo.")
public class UsuarioController {

	private final UsuarioService usuarioService;

	public UsuarioController(UsuarioService usuarioService) {
		this.usuarioService = usuarioService;
	}

	@GetMapping
	public PaginaResponse<UsuarioResponse> listar(@PageableDefault(size = 10, sort = "nome") Pageable pageable) {
		return usuarioService.listar(pageable);
	}

	@GetMapping("/me")
	public UsuarioResponse eu() {
		return usuarioService.eu();
	}

	@GetMapping("/{id}")
	public UsuarioResponse buscar(@PathVariable Long id) {
		return usuarioService.buscar(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public UsuarioResponse criar(@Valid @RequestBody UsuarioRequest request) {
		return usuarioService.criar(request);
	}

	@PutMapping("/{id}")
	public UsuarioResponse atualizar(@PathVariable Long id, @Valid @RequestBody UsuarioAtualizacaoRequest request) {
		return usuarioService.atualizar(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void excluir(@PathVariable Long id) {
		usuarioService.excluir(id);
	}

}
