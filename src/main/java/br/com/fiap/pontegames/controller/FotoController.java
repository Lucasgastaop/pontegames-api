package br.com.fiap.pontegames.controller;

import br.com.fiap.pontegames.service.ArmazenamentoFoto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fotos")
@Tag(name = "Fotos", description = "Arquivo JPG ou PNG enviado no descarte.")
public class FotoController {

	private final ArmazenamentoFoto armazenamentoFoto;

	public FotoController(ArmazenamentoFoto armazenamentoFoto) {
		this.armazenamentoFoto = armazenamentoFoto;
	}

	@GetMapping("/{nome:.+}")
	@Operation(summary = "Baixa a foto de um descarte. Usa o mesmo token das outras rotas.")
	public ResponseEntity<Resource> baixar(@PathVariable String nome) {
		ArmazenamentoFoto.FotoGuardada foto = armazenamentoFoto.abrir(nome);
		return ResponseEntity.ok()
				.contentType(foto.tipo())
				.header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + foto.nome() + "\"")
				.body(foto.recurso());
	}

}
