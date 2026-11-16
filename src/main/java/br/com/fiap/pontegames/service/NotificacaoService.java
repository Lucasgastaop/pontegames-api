package br.com.fiap.pontegames.service;

import br.com.fiap.pontegames.dto.NotificacaoResponse;
import br.com.fiap.pontegames.dto.PaginaResponse;
import br.com.fiap.pontegames.messaging.ResultadoDescarte;
import br.com.fiap.pontegames.model.Notificacao;
import br.com.fiap.pontegames.model.StatusValidacao;
import br.com.fiap.pontegames.model.Usuario;
import br.com.fiap.pontegames.repository.NotificacaoRepository;
import br.com.fiap.pontegames.repository.UsuarioRepository;
import br.com.fiap.pontegames.security.AutenticacaoAtual;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@Service
public class NotificacaoService {

	private static final Logger LOG = LoggerFactory.getLogger(NotificacaoService.class);

	private final NotificacaoRepository notificacaoRepository;
	private final UsuarioRepository usuarioRepository;
	private final AutenticacaoAtual autenticacaoAtual;

	public NotificacaoService(
			NotificacaoRepository notificacaoRepository,
			UsuarioRepository usuarioRepository,
			AutenticacaoAtual autenticacaoAtual
	) {
		this.notificacaoRepository = notificacaoRepository;
		this.usuarioRepository = usuarioRepository;
		this.autenticacaoAtual = autenticacaoAtual;
	}

	@Transactional(readOnly = true)
	public PaginaResponse<NotificacaoResponse> listar(Pageable pageable) {
		Long usuarioId = autenticacaoAtual.exigir().id();
		return PaginaResponse.de(notificacaoRepository.findByUsuario_Id(usuarioId, pageable).map(this::paraResponse));
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void registrar(ResultadoDescarte evento) {
		if (notificacaoRepository.existsByDescarteIdAndStatus(evento.descarteId(), evento.status())) {
			return;
		}
		Usuario usuario = usuarioRepository.findById(evento.usuarioId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário do aviso não encontrado."));
		Notificacao notificacao = new Notificacao();
		notificacao.setUsuario(usuario);
		notificacao.setDescarteId(evento.descarteId());
		notificacao.setStatus(evento.status());
		notificacao.setMensagem(mensagem(evento));
		notificacaoRepository.save(notificacao);
		LOG.info("Aviso do descarte {} gravado para o usuário {}.", evento.descarteId(), evento.usuarioId());
	}

	private String mensagem(ResultadoDescarte evento) {
		if (evento.status() == StatusValidacao.VALIDADO_IA) {
			return "Seu descarte de %s foi validado. Você ganhou %s XP e está no nível %d.".formatted(
					evento.nomeResiduo(),
					xp(evento.xpGanho()),
					evento.nivel() == null ? 1 : evento.nivel()
			);
		}
		return "Seu descarte de %s foi rejeitado. Nenhum XP foi creditado.".formatted(evento.nomeResiduo());
	}

	private String xp(BigDecimal valor) {
		if (valor == null) {
			return "0";
		}
		return valor.stripTrailingZeros().toPlainString();
	}

	private NotificacaoResponse paraResponse(Notificacao notificacao) {
		return new NotificacaoResponse(
				notificacao.getId(),
				notificacao.getDescarteId(),
				notificacao.getStatus(),
				notificacao.getMensagem(),
				notificacao.getDataCriacao()
		);
	}

}
