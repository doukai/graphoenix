package io.graphoenix.subscriptions.utils;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import io.graphoenix.subscriptions.config.RabbitMQConfig;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.RabbitFlux;
import reactor.rabbitmq.Receiver;
import reactor.rabbitmq.ReceiverOptions;
import reactor.rabbitmq.Sender;
import reactor.rabbitmq.SenderOptions;

import java.io.IOException;

public enum RabbitMQUtil {
    RABBIT_MQ_UTIL;

    public Mono<Connection> connectionMono(RabbitMQConfig rabbitMQConfig) {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.useNio();
        connectionFactory.setHost(rabbitMQConfig.getHost());
        connectionFactory.setPort(rabbitMQConfig.getPort());
        connectionFactory.setUsername(rabbitMQConfig.getUsername());
        connectionFactory.setPassword(rabbitMQConfig.getPassword());
        return Mono.fromCallable(connectionFactory::newConnection).cache();
    }

    public Sender sender(Mono<Connection> connectionMono) {
        return RabbitFlux.createSender(new SenderOptions().connectionMono(connectionMono));
    }

    public Receiver receiver(Mono<Connection> connectionMono) {
        return RabbitFlux.createReceiver(new ReceiverOptions().connectionMono(connectionMono));
    }

    public Mono<Void> close(Mono<Connection> connectionMono) {
        return connectionMono
                .flatMap(connection ->
                        Mono.fromRunnable(() -> {
                                    try {
                                        connection.close();
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                        )
                );
    }
}
