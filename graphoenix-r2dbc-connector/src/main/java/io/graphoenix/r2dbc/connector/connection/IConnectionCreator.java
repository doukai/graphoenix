package io.graphoenix.r2dbc.connector.connection;

import io.r2dbc.spi.Connection;
import reactor.core.publisher.Mono;

public interface IConnectionCreator {

    Mono<Connection> createConnection();
}
