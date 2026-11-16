package br.com.fiap.pontegames.service;

import br.com.fiap.pontegames.dto.PaginaResponse;
import br.com.fiap.pontegames.dto.PontoColetaRequest;
import br.com.fiap.pontegames.dto.PontoColetaResponse;
import br.com.fiap.pontegames.model.PontoColeta;
import br.com.fiap.pontegames.repository.PontoColetaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PontoColetaService {

	private final PontoColetaRepository pontoColetaRepository;

	public PontoColetaService(PontoColetaRepository pontoColetaRepository) {
		this.pontoColetaRepository = pontoColetaRepository;
	}

	@Transactional(readOnly = true)
	public PaginaResponse<PontoColetaResponse> listar(Pageable pageable) {
		return PaginaResponse.de(pontoColetaRepository.findAll(pageable).map(this::paraResponse));
	}

	@Transactional(readOnly = true)
	public PontoColetaResponse buscar(Long id) {
		return paraResponse(buscarEntidade(id));
	}

	@Transactional
	public PontoColetaResponse criar(PontoColetaRequest request) {
		PontoColeta ponto = new PontoColeta();
		aplicar(ponto, request);
		return paraResponse(pontoColetaRepository.save(ponto));
	}

	@Transactional
	public PontoColetaResponse atualizar(Long id, PontoColetaRequest request) {
		PontoColeta ponto = buscarEntidade(id);
		aplicar(ponto, request);
		return paraResponse(pontoColetaRepository.save(ponto));
	}

	@Transactional
	public void excluir(Long id) {
		PontoColeta ponto = buscarEntidade(id);
		pontoColetaRepository.delete(ponto);
		try {
			pontoColetaRepository.flush();
		} catch (DataIntegrityViolationException ex) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Ecoponto possui descartes e não pode ser excluído.");
		}
	}

	private void aplicar(PontoColeta ponto, PontoColetaRequest request) {
		ponto.setNome(limpar(request.nome()));
		ponto.setEndereco(limpar(request.endereco()));
		ponto.setLatitude(request.latitude());
		ponto.setLongitude(request.longitude());
		ponto.setAtivo(request.ativo() == null || request.ativo());
	}

	private PontoColeta buscarEntidade(Long id) {
		return pontoColetaRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ecoponto não encontrado."));
	}

	private PontoColetaResponse paraResponse(PontoColeta ponto) {
		return new PontoColetaResponse(
				ponto.getId(),
				ponto.getNome(),
				ponto.getEndereco(),
				ponto.getLatitude(),
				ponto.getLongitude(),
				ponto.isAtivo()
		);
	}

	private String limpar(String valor) {
		if (valor == null) {
			return null;
		}
		String limpo = valor.trim();
		return limpo.isEmpty() ? null : limpo;
	}

}
