package br.com.fiap.pontegames.service;

import br.com.fiap.pontegames.dto.DescarteRequest;
import br.com.fiap.pontegames.dto.DescarteResponse;
import br.com.fiap.pontegames.dto.PaginaResponse;
import br.com.fiap.pontegames.model.Descarte;
import br.com.fiap.pontegames.model.PontoColeta;
import br.com.fiap.pontegames.model.TipoResiduo;
import br.com.fiap.pontegames.model.Usuario;
import br.com.fiap.pontegames.repository.DescarteRepository;
import br.com.fiap.pontegames.repository.PontoColetaRepository;
import br.com.fiap.pontegames.repository.TipoResiduoRepository;
import br.com.fiap.pontegames.repository.UsuarioRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DescarteService {

	private final DescarteRepository descarteRepository;
	private final UsuarioRepository usuarioRepository;
	private final PontoColetaRepository pontoColetaRepository;
	private final TipoResiduoRepository tipoResiduoRepository;

	public DescarteService(
			DescarteRepository descarteRepository,
			UsuarioRepository usuarioRepository,
			PontoColetaRepository pontoColetaRepository,
			TipoResiduoRepository tipoResiduoRepository
	) {
		this.descarteRepository = descarteRepository;
		this.usuarioRepository = usuarioRepository;
		this.pontoColetaRepository = pontoColetaRepository;
		this.tipoResiduoRepository = tipoResiduoRepository;
	}

	@Transactional(readOnly = true)
	public PaginaResponse<DescarteResponse> listar(Pageable pageable) {
		return PaginaResponse.de(descarteRepository.findAll(pageable).map(this::paraResponse));
	}

	@Transactional(readOnly = true)
	public DescarteResponse buscar(Long id) {
		return paraResponse(buscarEntidade(id));
	}

	@Transactional
	public DescarteResponse criar(DescarteRequest request) {
		Descarte descarte = new Descarte();
		aplicar(descarte, request);
		return paraResponse(descarteRepository.save(descarte));
	}

	@Transactional
	public DescarteResponse atualizar(Long id, DescarteRequest request) {
		Descarte descarte = buscarEntidade(id);
		aplicar(descarte, request);
		return paraResponse(descarteRepository.save(descarte));
	}

	@Transactional
	public void excluir(Long id) {
		Descarte descarte = buscarEntidade(id);
		descarteRepository.delete(descarte);
	}

	private void aplicar(Descarte descarte, DescarteRequest request) {
		descarte.setUsuario(buscarUsuario(request.usuarioId()));
		descarte.setPontoColeta(buscarPonto(request.pontoId()));
		descarte.setTipoResiduo(buscarTipo(request.tipoResiduoId()));
		descarte.setPesoKg(request.pesoKg());
		descarte.setFotoUrl(limpar(request.fotoUrl()));
	}

	private Usuario buscarUsuario(Long id) {
		if (id == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usuário do descarte é obrigatório.");
		}
		return usuarioRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
	}

	private PontoColeta buscarPonto(Long id) {
		if (id == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ecoponto do descarte é obrigatório.");
		}
		return pontoColetaRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ecoponto não encontrado."));
	}

	private TipoResiduo buscarTipo(Long id) {
		if (id == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de resíduo do descarte é obrigatório.");
		}
		return tipoResiduoRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tipo de resíduo não encontrado."));
	}

	private Descarte buscarEntidade(Long id) {
		return descarteRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Descarte não encontrado."));
	}

	private DescarteResponse paraResponse(Descarte descarte) {
		return new DescarteResponse(
				descarte.getId(),
				descarte.getUsuario().getId(),
				descarte.getUsuario().getNome(),
				descarte.getPontoColeta().getId(),
				descarte.getPontoColeta().getNome(),
				descarte.getTipoResiduo().getId(),
				descarte.getTipoResiduo().getNome(),
				descarte.getPesoKg(),
				descarte.getXpGanho(),
				descarte.getFotoUrl(),
				descarte.getStatus(),
				descarte.getDataDescarte()
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
