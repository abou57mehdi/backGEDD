package com.smartged.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DocumentEventPublisher {

	private final RabbitTemplate rabbitTemplate;
	private final String exchange;
	private final String routingKey;

	public DocumentEventPublisher(
			RabbitTemplate rabbitTemplate,
			@Value("${app.messaging.documents.exchange}") String exchange,
			@Value("${app.messaging.documents.routing}") String routingKey
	) {
		this.rabbitTemplate = rabbitTemplate;
		this.exchange = exchange;
		this.routingKey = routingKey;
	}

	public void publish(DocumentUploadedEvent event) {
		rabbitTemplate.convertAndSend(exchange, routingKey, event);
	}
}


