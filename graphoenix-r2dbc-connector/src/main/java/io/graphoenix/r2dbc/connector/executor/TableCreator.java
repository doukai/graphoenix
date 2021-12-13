package io.graphoenix.r2dbc.connector.executor;

import io.graphoenix.r2dbc.connector.connection.IConnectionCreator;
import io.r2dbc.spi.Batch;
import reactor.core.publisher.Mono;

import javax.inject.Inject;
import java.util.stream.Stream;

public class TableCreator {

    private final IConnectionCreator connectionCreator;

    @Inject
    public TableCreator(IConnectionCreator connectionCreator) {
        this.connectionCreator = connectionCreator;
    }

    public Mono<Integer> createTable(String sql) {
        return null;
//        return connectionCreator.createConnection()
//                .flatMap(connection -> Mono.from(connection.createStatement(sql).execute()).doFinally(signalType -> connection.close()))
//                .flatMap(result -> Mono.from(result.getRowsUpdated()));
    }

    public Mono<Integer> createTables(Stream<String> sqlStream) {
        return null;
//        return connectionCreator.createConnection()
//                .flatMap(connection -> {
//                    Batch batch = connection.createBatch();
//                    sqlStream.forEach(batch::add);
//                    return Mono.from(batch.execute()).doFinally(signalType -> connection.close())
//                            .flatMap(result -> Mono.from(result.getRowsUpdated()));
//                });
    }
}
