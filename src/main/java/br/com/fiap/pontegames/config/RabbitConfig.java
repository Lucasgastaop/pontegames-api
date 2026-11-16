package br.com.fiap.pontegames.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
public class RabbitConfig {

	public static final String FILA_RESULTADO = "pontegames.descarte.resultado";

	@Bean
	Queue filaResultadoDescarte() {
		return new Queue(FILA_RESULTADO, true);
	}

	@Bean
	MessageConverter jacksonJsonMessageConverter() {
		return new JacksonJsonMessageConverter("br.com.fiap.pontegames.messaging");
	}

}
