package com.smartged.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DocumentEventsConfig {

	@Bean
	public TopicExchange documentsExchange(@Value("${app.messaging.documents.exchange}") String exchange) {
		return new TopicExchange(exchange, true, false);
	}

	@Bean
	public Queue documentUploadedQueue(@Value("${app.messaging.documents.queue}") String queue) {
		return new Queue(queue, true);
	}

	@Bean
	public Binding documentUploadedBinding(
			TopicExchange documentsExchange,
			Queue documentUploadedQueue,
			@Value("${app.messaging.documents.routing}") String routingKey
	) {
		return BindingBuilder.bind(documentUploadedQueue).to(documentsExchange).with(routingKey);
	}
}


