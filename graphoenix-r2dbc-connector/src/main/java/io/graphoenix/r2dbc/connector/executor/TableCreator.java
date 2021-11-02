package io.graphoenix.r2dbc.connector.executor;

import io.graphoenix.r2dbc.connector.connection.IConnectionCreator;
import io.r2dbc.spi.Batch;
import io.r2dbc.spi.Result;
import reactor.core.publisher.Mono;

import java.util.List;

public class TableCreator {

    private final IConnectionCreator connectionCreator;

    public TableCreator(IConnectionCreator connectionCreator) {
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
