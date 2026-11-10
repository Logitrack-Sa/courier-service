package com.logitrack.courierservice.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfiguration {
    public static final String SHIPMENT_EXCHANGE = "shipment.topic.exchange";
    public static final String SHIPMENT_STATUS_ROUTING_KEY = "shipment.status.changed";
    public static final String SHIPMENT_STATUS_QUEUE = "courier.shipment-status.events.queue";
    public static final String DEAD_LETTER_EXCHANGE = "courier.shipment-status.dlx";
    public static final String DEAD_LETTER_QUEUE = "courier.shipment-status.dlq";
    public static final String DEAD_LETTER_ROUTING_KEY = "courier.shipment-status.failed";

    @Bean
    TopicExchange shipmentTopicExchange() {
        return ExchangeBuilder.topicExchange(SHIPMENT_EXCHANGE).durable(true).build();
    }

    @Bean
    DirectExchange shipmentStatusDeadLetterExchange() {
        return ExchangeBuilder.directExchange(DEAD_LETTER_EXCHANGE).durable(true).build();
    }

    @Bean
    Queue shipmentStatusQueue() {
        return QueueBuilder.durable(SHIPMENT_STATUS_QUEUE)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(DEAD_LETTER_ROUTING_KEY)
                .build();
    }

    @Bean
    Queue shipmentStatusDeadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    @Bean
    Binding shipmentStatusBinding(Queue shipmentStatusQueue,
            TopicExchange shipmentTopicExchange) {
        return BindingBuilder.bind(shipmentStatusQueue).to(shipmentTopicExchange)
                .with(SHIPMENT_STATUS_ROUTING_KEY);
    }

    @Bean
    Binding shipmentStatusDeadLetterBinding(Queue shipmentStatusDeadLetterQueue,
            DirectExchange shipmentStatusDeadLetterExchange) {
        return BindingBuilder.bind(shipmentStatusDeadLetterQueue)
                .to(shipmentStatusDeadLetterExchange).with(DEAD_LETTER_ROUTING_KEY);
    }

    @Bean
    MessageConverter jackson2JsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
