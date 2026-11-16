package br.com.fiap.pontegames.messaging;

import br.com.fiap.pontegames.service.NotificacaoService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("test")
public class NotificadorDescarteDireto implements NotificadorDescarte {

	private final NotificacaoService notificacaoService;

	public NotificadorDescarteDireto(NotificacaoService notificacaoService) {
		this.notificacaoService = notificacaoService;
	}

	@Override
	public void publicar(ResultadoDescarte evento) {
		notificacaoService.registrar(evento);
	}

}
