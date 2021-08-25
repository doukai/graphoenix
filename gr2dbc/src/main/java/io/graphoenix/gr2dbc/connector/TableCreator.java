package io.graphoenix.gr2dbc.connector;

import io.r2dbc.spi.Result;
import reactor.core.publisher.Mono;

public class TableCreator {

    private final ConnectionCreator connectionCreator;

    public TableCreator(ConnectionCreator connectionCreator) {
        this.connectionCreator = connectionCreator;
    }

    public Mono<Result> createTable(String sql) {
        return connectionCreator.createConnection()
                .flatMap(connection -> Mono.from(connection.createStatement(sql).execute()).doFinally(signalType -> connection.close()));
    }
}
