package br.com.fiap.pontegames.messaging;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PublicacaoResultadoDescarte {

	private final ApplicationEventPublisher publicador;
	private final NotificadorDescarte notificadorDescarte;

	public PublicacaoResultadoDescarte(
			ApplicationEventPublisher publicador,
			NotificadorDescarte notificadorDescarte
	) {
		this.publicador = publicador;
		this.notificadorDescarte = notificadorDescarte;
	}

	public void agendar(ResultadoDescarte evento) {
		publicador.publishEvent(evento);
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void publicar(ResultadoDescarte evento) {
		notificadorDescarte.publicar(evento);
	}

}
