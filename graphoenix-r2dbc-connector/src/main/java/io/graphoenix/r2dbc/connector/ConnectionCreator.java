package io.graphoenix.r2dbc.connector;

import io.r2dbc.spi.Connection;
import reactor.core.publisher.Mono;

public interface ConnectionCreator {

    Mono<Connection> createConnection();
}
