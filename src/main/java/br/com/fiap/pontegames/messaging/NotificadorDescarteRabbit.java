package br.com.fiap.pontegames.messaging;

import br.com.fiap.pontegames.config.RabbitConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("!test")
public class NotificadorDescarteRabbit implements NotificadorDescarte {

	private static final Logger LOG = LoggerFactory.getLogger(NotificadorDescarteRabbit.class);

	private final RabbitTemplate rabbitTemplate;

	public NotificadorDescarteRabbit(RabbitTemplate rabbitTemplate) {
		this.rabbitTemplate = rabbitTemplate;
	}

	@Override
	public void publicar(ResultadoDescarte evento) {
		rabbitTemplate.convertAndSend(RabbitConfig.FILA_RESULTADO, evento);
		LOG.info("Resultado do descarte {} publicado na fila {}.", evento.descarteId(), RabbitConfig.FILA_RESULTADO);
	}

}
