package io.graphoenix.r2dbc.connector.executor;

import io.graphoenix.r2dbc.connector.connection.IConnectionCreator;
import io.r2dbc.spi.Batch;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.stream.Stream;

public class MutationExecutor {

    private final IConnectionCreator connectionCreator;

    public MutationExecutor(IConnectionCreator connectionCreator) {
        this.connectionCreator = connectionCreator;
    }

    public Mono<String> executeMutationsInBatch(Stream<String> sqlStream) {
        return connectionCreator.createConnection()
                .flatMap(connection -> {
                    connection.beginTransaction();
                    Batch batch = connection.createBatch();
                    sqlStream.forEach(batch::add);
                    return Flux.from(batch.execute())
                            .last()
                            .doOnSuccess(result -> connection.commitTransaction())
                            .flatMap(result -> Mono.from(result.map((row, rowMetadata) -> row.get(0, String.class))))
                            .doOnError(throwable -> connection.rollbackTransaction())
                            .doFinally(signalType -> connection.close());
                });
    }

    public Mono<String> executeMutations(Stream<String> sqlStream) {
        return connectionCreator.createConnection()
                .flatMap(connection -> {
                    connection.beginTransaction();
                    return Flux.fromStream(sqlStream.map(sql -> Mono.from(connection.createStatement(sql).execute()).block()))
                            .last()
                            .flatMap(result -> Mono.from(result.map((row, rowMetadata) -> row.get(0, String.class))))
                            .doOnSuccess(result -> connection.commitTransaction())
                            .doOnError(throwable -> connection.rollbackTransaction())
                            .doFinally(signalType -> connection.close());
                });
    }
}
