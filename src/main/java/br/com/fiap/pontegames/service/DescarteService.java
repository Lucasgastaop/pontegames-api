package br.com.fiap.pontegames.service;

import br.com.fiap.pontegames.dto.DescarteRequest;
import br.com.fiap.pontegames.dto.DescarteResponse;
import br.com.fiap.pontegames.dto.PaginaResponse;
import br.com.fiap.pontegames.model.Descarte;
import br.com.fiap.pontegames.model.PontoColeta;
import br.com.fiap.pontegames.model.StatusValidacao;
import br.com.fiap.pontegames.model.TipoResiduo;
import br.com.fiap.pontegames.model.TipoUsuario;
import br.com.fiap.pontegames.model.Usuario;
import br.com.fiap.pontegames.repository.DescarteRepository;
import br.com.fiap.pontegames.repository.PontoColetaRepository;
import br.com.fiap.pontegames.repository.TipoResiduoRepository;
import br.com.fiap.pontegames.repository.UsuarioRepository;
import br.com.fiap.pontegames.security.AutenticacaoAtual;
import br.com.fiap.pontegames.security.UsuarioAutenticado;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class DescarteService {

	private static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");
	private static final long SEM_ID = -1L;

	private final DescarteRepository descarteRepository;
	private final UsuarioRepository usuarioRepository;
	private final PontoColetaRepository pontoColetaRepository;
	private final TipoResiduoRepository tipoResiduoRepository;
	private final AutenticacaoAtual autenticacaoAtual;
	private final GamificacaoService gamificacaoService;
	private final BancoOracle bancoOracle;
	private final EntityManager entityManager;

	public DescarteService(
			DescarteRepository descarteRepository,
			UsuarioRepository usuarioRepository,
			PontoColetaRepository pontoColetaRepository,
			TipoResiduoRepository tipoResiduoRepository,
			AutenticacaoAtual autenticacaoAtual,
			GamificacaoService gamificacaoService,
			BancoOracle bancoOracle,
			EntityManager entityManager
	) {
		this.descarteRepository = descarteRepository;
		this.usuarioRepository = usuarioRepository;
		this.pontoColetaRepository = pontoColetaRepository;
		this.tipoResiduoRepository = tipoResiduoRepository;
		this.autenticacaoAtual = autenticacaoAtual;
		this.gamificacaoService = gamificacaoService;
		this.bancoOracle = bancoOracle;
		this.entityManager = entityManager;
	}

	@Transactional(readOnly = true)
	public PaginaResponse<DescarteResponse> listar(Pageable pageable) {
		UsuarioAutenticado atual = autenticacaoAtual.exigir();
		var pagina = atual.tipo() == TipoUsuario.CIDADAO
				? descarteRepository.findByUsuario_Id(atual.id(), pageable)
				: descarteRepository.findAll(pageable);
		return PaginaResponse.de(pagina.map(this::paraResponse));
	}

	@Transactional(readOnly = true)
	public DescarteResponse buscar(Long id) {
		Descarte descarte = buscarEntidade(id);
		garantirProprioDescarte(descarte.getUsuario().getId());
		return paraResponse(descarte);
	}

	@Transactional
	public DescarteResponse criar(DescarteRequest request) {
		garantirProprioDescarte(request.usuarioId());
		Descarte descarte = new Descarte();
		aplicar(descarte, request);
		descarte.setStatus(StatusValidacao.PENDENTE);
		aplicarRegras(descarte, SEM_ID);
		return paraResponse(descarteRepository.save(descarte));
	}

	@Transactional
	public DescarteResponse atualizar(Long id, DescarteRequest request) {
		Descarte descarte = buscarEntidade(id);
		garantirProprioDescarte(descarte.getUsuario().getId());
		garantirProprioDescarte(request.usuarioId());
		if (descarte.getStatus() != StatusValidacao.PENDENTE) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Somente um descarte pendente pode ser alterado.");
		}
		aplicar(descarte, request);
		aplicarRegras(descarte, descarte.getId());
		return paraResponse(descarteRepository.save(descarte));
	}

	@Transactional
	public DescarteResponse validar(Long id, StatusValidacao novoStatus) {
		garantirGestor();
		if (novoStatus != StatusValidacao.VALIDADO_IA && novoStatus != StatusValidacao.REJEITADO) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status permitido na validação: VALIDADO_IA ou REJEITADO.");
		}
		Descarte descarte = buscarEntidade(id);
		if (descarte.getStatus() != StatusValidacao.PENDENTE) {
			throw new ResponseStatusException(
					HttpStatus.CONFLICT,
					"Somente um descarte pendente pode ser validado. Status atual: " + descarte.getStatus() + "."
			);
		}
		descarte.setStatus(novoStatus);
		if (novoStatus == StatusValidacao.VALIDADO_IA) {
			creditarSeAplicavel(descarte.getUsuario(), descarte.getXpGanho());
		}
		Descarte salvo = descarteRepository.saveAndFlush(descarte);
		sincronizarSaldo(salvo.getUsuario());
		return paraResponse(salvo);
	}

	@Transactional
	public void excluir(Long id) {
		Descarte descarte = buscarEntidade(id);
		garantirProprioDescarte(descarte.getUsuario().getId());
		boolean validado = descarte.getStatus() == StatusValidacao.VALIDADO_IA;
		Usuario usuario = descarte.getUsuario();
		BigDecimal xp = descarte.getXpGanho();
		descarteRepository.delete(descarte);
		if (validado) {
			estornarSeAplicavel(usuario, xp);
			descarteRepository.flush();
			sincronizarSaldo(usuario);
		}
	}

	private void aplicarRegras(Descarte descarte, Long idIgnorado) {
		if (!descarte.getPontoColeta().isAtivo()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Ecoponto inativo não recebe descarte.");
		}
		BigDecimal peso = descarte.getPesoKg().setScale(2, RoundingMode.HALF_UP);
		descarte.setPesoKg(peso);
		descarte.setXpGanho(gamificacaoService.calcularXp(descarte.getTipoResiduo(), peso));
		LocalDateTime inicio = LocalDate.now(ZONA).atStartOfDay();
		LocalDateTime fim = inicio.plusDays(1);
		BigDecimal acumulado = descarteRepository.somarPesoNoDia(
				descarte.getUsuario().getId(),
				descarte.getTipoResiduo().getId(),
				inicio,
				fim,
				idIgnorado
		);
		if (acumulado == null) {
			acumulado = BigDecimal.ZERO;
		}
		BigDecimal limite = gamificacaoService.limiteDiarioKg(descarte.getTipoResiduo());
		if (acumulado.add(peso).compareTo(limite) > 0) {
			throw new ResponseStatusException(
					HttpStatus.CONFLICT,
					"Limite diário excedido para este resíduo. Limite: " + limite.stripTrailingZeros().toPlainString() + " kg."
			);
		}
		long repetidos = descarteRepository.contarDuplicadoNoDia(
				descarte.getUsuario().getId(),
				descarte.getPontoColeta().getId(),
				descarte.getTipoResiduo().getId(),
				peso,
				inicio,
				fim,
				idIgnorado
		);
		if (repetidos > 0) {
			throw new ResponseStatusException(
					HttpStatus.CONFLICT,
					"Descarte duplicado no mesmo dia para o mesmo cidadão, ecoponto, tipo e peso."
			);
		}
	}

	private void creditarSeAplicavel(Usuario usuario, BigDecimal xp) {
		if (bancoOracle.creditoFeitoPeloTrigger()) {
			return;
		}
		ajustarSaldo(usuario, xp);
	}

	private void estornarSeAplicavel(Usuario usuario, BigDecimal xp) {
		if (bancoOracle.creditoFeitoPeloTrigger()) {
			return;
		}
		ajustarSaldo(usuario, xp.negate());
	}

	private void ajustarSaldo(Usuario usuario, BigDecimal delta) {
		BigDecimal saldo = usuario.getXpTotal().add(delta);
		if (saldo.signum() < 0) {
			saldo = BigDecimal.ZERO;
		}
		usuario.setXpTotal(saldo.setScale(2, RoundingMode.HALF_UP));
		usuario.setNivel(gamificacaoService.nivel(saldo));
		usuarioRepository.save(usuario);
	}

	private void sincronizarSaldo(Usuario usuario) {
		if (bancoOracle.creditoFeitoPeloTrigger()) {
			entityManager.refresh(usuario);
		}
	}

	private void garantirGestor() {
		UsuarioAutenticado atual = autenticacaoAtual.exigir();
		if (atual.tipo() == TipoUsuario.CIDADAO) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas gestor ou admin podem validar um descarte.");
		}
	}

	private void garantirProprioDescarte(Long usuarioDoDescarte) {
		UsuarioAutenticado atual = autenticacaoAtual.exigir();
		if (atual.tipo() == TipoUsuario.CIDADAO && !atual.id().equals(usuarioDoDescarte)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cidadão só acessa o próprio descarte.");
		}
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
