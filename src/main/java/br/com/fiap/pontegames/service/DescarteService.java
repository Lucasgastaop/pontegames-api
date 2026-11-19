package br.com.fiap.pontegames.service;

import br.com.fiap.pontegames.dto.DescarteRequest;
import br.com.fiap.pontegames.messaging.PublicacaoResultadoDescarte;
import br.com.fiap.pontegames.messaging.ResultadoDescarte;
import br.com.fiap.pontegames.dto.DescarteResponse;
import br.com.fiap.pontegames.dto.PaginaResponse;
import br.com.fiap.pontegames.model.Descarte;
import br.com.fiap.pontegames.model.ParecerIa;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

@Service
public class DescarteService {

	private static final Logger LOG = LoggerFactory.getLogger(DescarteService.class);
	private static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");
	private static final long SEM_ID = -1L;
	private static final String PARECER_INDISPONIVEL = "Não foi possível analisar a foto agora. Envie outra imagem.";

	private final DescarteRepository descarteRepository;
	private final UsuarioRepository usuarioRepository;
	private final PontoColetaRepository pontoColetaRepository;
	private final TipoResiduoRepository tipoResiduoRepository;
	private final AutenticacaoAtual autenticacaoAtual;
	private final GamificacaoService gamificacaoService;
	private final AnalisadorFoto analisadorFoto;
	private final ArmazenamentoFoto armazenamentoFoto;
	private final PublicacaoResultadoDescarte publicacaoResultadoDescarte;
	private final BancoOracle bancoOracle;
	private final EntityManager entityManager;
	private final TransactionTemplate transacao;

	public DescarteService(
			DescarteRepository descarteRepository,
			UsuarioRepository usuarioRepository,
			PontoColetaRepository pontoColetaRepository,
			TipoResiduoRepository tipoResiduoRepository,
			AutenticacaoAtual autenticacaoAtual,
			GamificacaoService gamificacaoService,
			AnalisadorFoto analisadorFoto,
			ArmazenamentoFoto armazenamentoFoto,
			PublicacaoResultadoDescarte publicacaoResultadoDescarte,
			BancoOracle bancoOracle,
			EntityManager entityManager,
			PlatformTransactionManager transacoes
	) {
		this.descarteRepository = descarteRepository;
		this.usuarioRepository = usuarioRepository;
		this.pontoColetaRepository = pontoColetaRepository;
		this.tipoResiduoRepository = tipoResiduoRepository;
		this.autenticacaoAtual = autenticacaoAtual;
		this.gamificacaoService = gamificacaoService;
		this.analisadorFoto = analisadorFoto;
		this.armazenamentoFoto = armazenamentoFoto;
		this.publicacaoResultadoDescarte = publicacaoResultadoDescarte;
		this.bancoOracle = bancoOracle;
		this.entityManager = entityManager;
		this.transacao = new TransactionTemplate(transacoes);
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

	public DescarteResponse criar(DescarteRequest request, MultipartFile foto) {
		garantirProprioDescarte(request.usuarioId());
		ArmazenamentoFoto.FotoRecebida recebida = armazenamentoFoto.ler(foto);
		ParecerFoto parecer = analisarAntesDeGravar(request, recebida);
		return transacao.execute(status -> gravar(new Descarte(), request, recebida, parecer, true));
	}

	public DescarteResponse atualizar(Long id, DescarteRequest request, MultipartFile foto) {
		garantirProprioDescarte(request.usuarioId());
		transacao.executeWithoutResult(status -> conferirPendente(id));
		ArmazenamentoFoto.FotoRecebida recebida = armazenamentoFoto.ler(foto);
		ParecerFoto parecer = analisarAntesDeGravar(request, recebida);
		return transacao.execute(status -> {
			conferirPendente(id);
			return gravar(buscarEntidade(id), request, recebida, parecer, false);
		});
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
		return paraResponse(fechar(descarte, novoStatus));
	}

	@Transactional
	public void excluir(Long id) {
		Descarte descarte = buscarEntidade(id);
		garantirProprioDescarte(descarte.getUsuario().getId());
		boolean validado = descarte.getStatus() == StatusValidacao.VALIDADO_IA;
		Usuario usuario = descarte.getUsuario();
		BigDecimal xp = descarte.getXpGanho();
		String foto = descarte.getFotoUrl();
		descarteRepository.delete(descarte);
		if (validado) {
			estornarSeAplicavel(usuario, xp);
			descarteRepository.flush();
			sincronizarSaldo(usuario);
		}
		armazenamentoFoto.apagarSeNossa(foto);
	}

	private void conferirPendente(Long id) {
		Descarte descarte = buscarEntidade(id);
		garantirProprioDescarte(descarte.getUsuario().getId());
		if (descarte.getStatus() != StatusValidacao.PENDENTE) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Somente um descarte pendente pode ser alterado.");
		}
	}

	private ParecerFoto analisarAntesDeGravar(DescarteRequest request, ArmazenamentoFoto.FotoRecebida foto) {
		String url = limpar(request.fotoUrl());
		if (foto == null && url == null) {
			return null;
		}
		TipoResiduo tipo = transacao.execute(status -> buscarTipo(request.tipoResiduoId()));
		try {
			ParecerFoto parecer = foto == null
					? analisadorFoto.analisar(tipo.getNome(), tipo.getInstrucao(), url)
					: analisadorFoto.analisar(tipo.getNome(), tipo.getInstrucao(), foto.conteudo(), foto.nomeArquivo());
			ParecerIa resultado = parecer == null || parecer.resultado() == null
					? ParecerIa.INCONCLUSIVO
					: parecer.resultado();
			String mensagem = parecer == null ? null : parecer.mensagem();
			if (mensagem == null || mensagem.isBlank()) {
				mensagem = mensagemPadrao(resultado, tipo.getNome());
			}
			return new ParecerFoto(resultado, encurtar(mensagem));
		} catch (Exception ex) {
			LOG.warn("Falha ao analisar a foto do descarte: {}", ex.getMessage());
			return new ParecerFoto(ParecerIa.INCONCLUSIVO, PARECER_INDISPONIVEL);
		}
	}

	private DescarteResponse gravar(
			Descarte descarte,
			DescarteRequest request,
			ArmazenamentoFoto.FotoRecebida recebida,
			ParecerFoto parecer,
			boolean criacao
	) {
		String fotoAnterior = descarte.getFotoUrl();
		aplicar(descarte, request);
		if (criacao) {
			descarte.setStatus(StatusValidacao.PENDENTE);
		}
		aplicarRegras(descarte, criacao ? SEM_ID : descarte.getId());
		String nova = null;
		try {
			if (recebida != null) {
				nova = armazenamentoFoto.guardar(recebida);
				descarte.setFotoUrl(nova);
			}
			aplicarParecer(descarte, parecer);
			Descarte salvo = descarteRepository.saveAndFlush(descarte);
			salvo = decidirPeloParecer(salvo);
			if (!Objects.equals(fotoAnterior, salvo.getFotoUrl())) {
				armazenamentoFoto.apagarSeNossa(fotoAnterior);
			}
			return paraResponse(salvo);
		} catch (RuntimeException ex) {
			armazenamentoFoto.apagarSeNossa(nova);
			throw ex;
		}
	}

	private void aplicarParecer(Descarte descarte, ParecerFoto parecer) {
		if (parecer == null) {
			descarte.setParecer(null);
			descarte.setMensagemParecer(null);
			return;
		}
		descarte.setParecer(parecer.resultado());
		descarte.setMensagemParecer(parecer.mensagem());
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

	private String mensagemPadrao(ParecerIa resultado, String nomeResiduo) {
		return switch (resultado) {
			case COMPATIVEL -> "A foto parece compatível com " + nomeResiduo + ".";
			case INCOMPATIVEL -> "A foto não parece ser " + nomeResiduo + ".";
			case INCONCLUSIVO -> "A foto não foi suficiente para confirmar " + nomeResiduo + ".";
		};
	}

	private String encurtar(String mensagem) {
		String limpa = mensagem.trim();
		if (limpa.length() > 240) {
			limpa = limpa.substring(0, 240).trim();
		}
		while (limpa.getBytes(StandardCharsets.UTF_8).length > 450 && !limpa.isEmpty()) {
			limpa = limpa.substring(0, limpa.length() - 1).trim();
		}
		return limpa;
	}

	private Descarte decidirPeloParecer(Descarte descarte) {
		if (descarte.getStatus() != StatusValidacao.PENDENTE) {
			return descarte;
		}
		if (descarte.getParecer() == ParecerIa.COMPATIVEL) {
			return fechar(descarte, StatusValidacao.VALIDADO_IA);
		}
		if (descarte.getParecer() == ParecerIa.INCOMPATIVEL) {
			return fechar(descarte, StatusValidacao.REJEITADO);
		}
		return descarte;
	}

	private Descarte fechar(Descarte descarte, StatusValidacao novoStatus) {
		descarte.setStatus(novoStatus);
		if (novoStatus == StatusValidacao.VALIDADO_IA) {
			creditarSeAplicavel(descarte.getUsuario(), descarte.getXpGanho());
		}
		Descarte salvo = descarteRepository.saveAndFlush(descarte);
		try {
			sincronizarSaldo(salvo.getUsuario());
		} catch (RuntimeException ex) {
			LOG.warn("Não foi possível reler o saldo depois do descarte: {}", ex.getMessage());
		}
		Usuario usuario = salvo.getUsuario();
		try {
			publicacaoResultadoDescarte.agendar(new ResultadoDescarte(
					salvo.getId(),
					usuario.getId(),
					usuario.getNome(),
					salvo.getTipoResiduo().getNome(),
					salvo.getXpGanho(),
					usuario.getXpTotal(),
					usuario.getNivel(),
					salvo.getStatus()
			));
		} catch (RuntimeException ex) {
			LOG.warn("Não foi possível agendar o aviso do descarte: {}", ex.getMessage());
		}
		return salvo;
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
				descarte.getParecer(),
				descarte.getMensagemParecer(),
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
