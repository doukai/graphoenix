package io.graphoenix.gr2dbc.connector;

import io.r2dbc.spi.Batch;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.Result;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public class TableCreator {

    private final ConnectionCreator connectionCreator;

    public TableCreator(ConnectionCreator connectionCreator) {
        this.connectionCreator = connectionCreator;
    }

    public Mono<Result> createTable(String sql) {
        return connectionCreator.createConnection()
                .flatMap(connection -> Mono.from(connection.createStatement(sql).execute()).doFinally(signalType -> connection.close()));
    }

    public Mono<Result> createTables(List<String> sqlList) {
        return connectionCreator.createConnection()
                .flatMap(connection -> {
                    Batch batch = connection.createBatch();
                    sqlList.forEach(batch::add);
                    return Mono.from(batch.execute()).doFinally(signalType -> connection.close());
                });
    }
}
