package io.graphoenix.r2dbc.connector.executor;

import io.graphoenix.r2dbc.connector.connection.ConnectionCreator;
import io.r2dbc.spi.Batch;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import reactor.core.publisher.Mono;

import java.util.stream.Stream;

@ApplicationScoped
public class TableCreator {

    private final ConnectionCreator connectionCreator;

    @Inject
    public TableCreator(ConnectionCreator connectionCreator) {
        this.connectionCreator = connectionCreator;
    }

    public Mono<Integer> createTable(String sql) {
        return connectionCreator.createConnection()
                .flatMap(connection -> Mono.from(connection.createStatement(sql).execute()).doFinally(signalType -> connection.close()))
                .flatMap(result -> Mono.from(result.getRowsUpdated()));
    }

    public Mono<Integer> createTables(Stream<String> sqlStream) {
        return connectionCreator.createConnection()
                .flatMap(connection -> {
                            Batch batch = connection.createBatch();
                            sqlStream.forEach(batch::add);
                            return Mono.from(batch.execute()).doFinally(signalType -> connection.close())
                                    .flatMap(result -> Mono.from(result.getRowsUpdated()));
                        }
                );
    }
}
