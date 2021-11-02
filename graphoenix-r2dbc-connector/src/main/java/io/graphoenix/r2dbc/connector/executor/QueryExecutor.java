package io.graphoenix.r2dbc.connector.executor;

import io.graphoenix.r2dbc.connector.connection.IConnectionCreator;
import reactor.core.publisher.Mono;

public class QueryExecutor {

    private final IConnectionCreator connectionCreator;

    public QueryExecutor(IConnectionCreator connectionCreator) {
        this.connectionCreator = connectionCreator;
    }

    public Mono<String> executeQuery(String sql) {
        return connectionCreator.createConnection()
                .flatMap(connection -> Mono.from(connection.createStatement(sql).execute()).doFinally(signalType -> connection.close()))
                .flatMap(result -> Mono.from(result.map((row, rowMetadata) -> row.get(0, String.class))));
    }
}
