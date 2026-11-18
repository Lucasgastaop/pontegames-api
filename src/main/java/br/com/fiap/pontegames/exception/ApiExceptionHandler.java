package br.com.fiap.pontegames.exception;

import br.com.fiap.pontegames.dto.CampoErro;
import br.com.fiap.pontegames.dto.ErroResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErroResponse> validacao(MethodArgumentNotValidException ex) {
		List<CampoErro> campos = ex.getBindingResult().getFieldErrors().stream()
				.map(erro -> new CampoErro(erro.getField(), erro.getDefaultMessage()))
				.toList();
		return resposta(HttpStatus.BAD_REQUEST, "Revise os campos informados.", campos);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErroResponse> corpoInvalido(HttpMessageNotReadableException ex) {
		return resposta(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido.", List.of());
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<ErroResponse> arquivoGrande(MaxUploadSizeExceededException ex) {
		return resposta(HttpStatus.BAD_REQUEST, "A foto deve ter no máximo 5 MB.", List.of());
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErroResponse> tipoInvalido(MethodArgumentTypeMismatchException ex) {
		return resposta(HttpStatus.BAD_REQUEST, "Valor inválido para " + ex.getName() + ".", List.of());
	}

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<ErroResponse> status(ResponseStatusException ex) {
		HttpStatus httpStatus = HttpStatus.resolve(ex.getStatusCode().value());
		if (httpStatus == null) {
			httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
		}
		String mensagem = ex.getReason() == null ? "Não foi possível concluir a operação." : ex.getReason();
		return resposta(httpStatus, mensagem, List.of());
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ErroResponse> integridade(DataIntegrityViolationException ex) {
		return resposta(HttpStatus.CONFLICT, "Operação viola uma regra já gravada no banco.", List.of());
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ErroResponse> rotaInexistente(NoResourceFoundException ex) {
		return resposta(HttpStatus.NOT_FOUND, "Recurso não encontrado.", List.of());
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErroResponse> inesperado(Exception ex) {
		log.error("Falha não tratada", ex);
		return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "Falha inesperada ao processar a requisição.", List.of());
	}

	private ResponseEntity<ErroResponse> resposta(HttpStatus status, String mensagem, List<CampoErro> campos) {
		ErroResponse corpo = new ErroResponse(status.value(), titulo(status), mensagem, campos);
		return ResponseEntity.status(status).body(corpo);
	}

	private String titulo(HttpStatus status) {
		return switch (status) {
			case BAD_REQUEST -> "Requisição inválida";
			case UNAUTHORIZED -> "Não autenticado";
			case FORBIDDEN -> "Acesso negado";
			case NOT_FOUND -> "Não encontrado";
			case CONFLICT -> "Conflito";
			case INTERNAL_SERVER_ERROR -> "Erro interno";
			default -> status.getReasonPhrase();
		};
	}

}
