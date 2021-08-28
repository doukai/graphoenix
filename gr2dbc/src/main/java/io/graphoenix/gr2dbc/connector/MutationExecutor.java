package io.graphoenix.gr2dbc.connector;

import io.r2dbc.spi.Batch;
import reactor.core.publisher.Mono;

import java.util.List;

public class MutationExecutor {

    private final ConnectionCreator connectionCreator;

    public MutationExecutor(ConnectionCreator connectionCreator) {
        this.connectionCreator = connectionCreator;
    }

    public Mono<String> executeMutations(List<String> sqlList) {
        return connectionCreator.createConnection()
                .flatMap(connection -> {
                    connection.beginTransaction();
                    Batch batch = connection.createBatch();
                    sqlList.forEach(batch::add);
                    return Mono.from(batch.execute())
                            .doOnSuccess(result -> connection.commitTransaction())
                            .doOnError(throwable -> connection.rollbackTransaction())
                            .doFinally(signalType -> connection.close());
                })
                .flatMap(result -> Mono.from(result.map((row, rowMetadata) -> row.get(0, String.class))));
    }
}
