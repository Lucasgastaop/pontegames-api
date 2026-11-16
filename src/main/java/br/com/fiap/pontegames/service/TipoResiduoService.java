package br.com.fiap.pontegames.service;

import br.com.fiap.pontegames.dto.PaginaResponse;
import br.com.fiap.pontegames.dto.TipoResiduoRequest;
import br.com.fiap.pontegames.dto.TipoResiduoResponse;
import br.com.fiap.pontegames.model.TipoResiduo;
import br.com.fiap.pontegames.repository.TipoResiduoRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TipoResiduoService {

	private final TipoResiduoRepository tipoResiduoRepository;

	public TipoResiduoService(TipoResiduoRepository tipoResiduoRepository) {
		this.tipoResiduoRepository = tipoResiduoRepository;
	}

	@Transactional(readOnly = true)
	public PaginaResponse<TipoResiduoResponse> listar(Pageable pageable) {
		return PaginaResponse.de(tipoResiduoRepository.findAll(pageable).map(this::paraResponse));
	}

	@Transactional(readOnly = true)
	public TipoResiduoResponse buscar(Long id) {
		return paraResponse(buscarEntidade(id));
	}

	@Transactional
	public TipoResiduoResponse criar(TipoResiduoRequest request) {
		TipoResiduo tipo = new TipoResiduo();
		aplicar(tipo, request);
		return paraResponse(tipoResiduoRepository.save(tipo));
	}

	@Transactional
	public TipoResiduoResponse atualizar(Long id, TipoResiduoRequest request) {
		TipoResiduo tipo = buscarEntidade(id);
		aplicar(tipo, request);
		return paraResponse(tipoResiduoRepository.save(tipo));
	}

	@Transactional
	public void excluir(Long id) {
		TipoResiduo tipo = buscarEntidade(id);
		tipoResiduoRepository.delete(tipo);
		try {
			tipoResiduoRepository.flush();
		} catch (DataIntegrityViolationException ex) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Tipo de resíduo possui descartes e não pode ser excluído.");
		}
	}

	private void aplicar(TipoResiduo tipo, TipoResiduoRequest request) {
		tipo.setNome(limpar(request.nome()));
		tipo.setInstrucao(limpar(request.instrucao()));
		tipo.setXpPorKg(request.xpPorKg());
	}

	private TipoResiduo buscarEntidade(Long id) {
		return tipoResiduoRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tipo de resíduo não encontrado."));
	}

	private TipoResiduoResponse paraResponse(TipoResiduo tipo) {
		return new TipoResiduoResponse(tipo.getId(), tipo.getNome(), tipo.getInstrucao(), tipo.getXpPorKg());
	}

	private String limpar(String valor) {
		if (valor == null) {
			return null;
		}
		String limpo = valor.trim();
		return limpo.isEmpty() ? null : limpo;
	}

}
