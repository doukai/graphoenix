package io.graphoenix.subscriptions.produces;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import io.graphoenix.subscriptions.config.RabbitMQConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.*;

@ApplicationScoped
public class RabbitMQProducer {

    private final RabbitMQConfig rabbitMQConfig;

    @Inject
    public RabbitMQProducer(RabbitMQConfig rabbitMQConfig) {
        this.rabbitMQConfig = rabbitMQConfig;
    }

    @Produces
    @ApplicationScoped
    public Mono<Connection> connectionMono() {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.useNio();
        connectionFactory.setHost(rabbitMQConfig.getHost());
        connectionFactory.setPort(rabbitMQConfig.getPort());
        connectionFactory.setUsername(rabbitMQConfig.getUsername());
        connectionFactory.setPassword(rabbitMQConfig.getPassword());
        return Mono.fromCallable(connectionFactory::newConnection).cache();
    }

    @Produces
    @ApplicationScoped
    public Sender sender() {
        return RabbitFlux.createSender(new SenderOptions().connectionMono(connectionMono()));
    }

    @Produces
    @ApplicationScoped
    public Receiver receiver() {
        return RabbitFlux.createReceiver(new ReceiverOptions().connectionMono(connectionMono()));
    }
}
