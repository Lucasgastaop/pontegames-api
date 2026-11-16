package br.com.fiap.pontegames.messaging;

import br.com.fiap.pontegames.config.RabbitConfig;
import br.com.fiap.pontegames.service.NotificacaoService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class ResultadoDescarteListener {

	private final NotificacaoService notificacaoService;

	public ResultadoDescarteListener(NotificacaoService notificacaoService) {
		this.notificacaoService = notificacaoService;
	}

	@RabbitListener(queues = RabbitConfig.FILA_RESULTADO)
	public void consumir(ResultadoDescarte evento) {
		notificacaoService.registrar(evento);
	}

}
