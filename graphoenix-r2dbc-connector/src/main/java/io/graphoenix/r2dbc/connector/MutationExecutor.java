package io.graphoenix.r2dbc.connector;

import io.graphoenix.r2dbc.connector.connection.IConnectionCreator;
import io.r2dbc.spi.Batch;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public class MutationExecutor {

    private final IConnectionCreator connectionCreator;

    public MutationExecutor(IConnectionCreator connectionCreator) {
        this.connectionCreator = connectionCreator;
    }

    public Mono<String> executeMutations(List<String> sqlList) {
        return connectionCreator.createConnection()
                .flatMap(connection -> {
                    connection.beginTransaction();
                    Batch batch = connection.createBatch();
                    sqlList.forEach(batch::add);
                    return Flux.from(batch.execute())
                            .last()
                            .doOnSuccess(result -> connection.commitTransaction())
                            .flatMap(result -> Mono.from(result.map((row, rowMetadata) -> row.get(0, String.class))))
                            .doOnError(throwable -> connection.rollbackTransaction())
                            .doFinally(signalType -> connection.close());
                });
    }
}
