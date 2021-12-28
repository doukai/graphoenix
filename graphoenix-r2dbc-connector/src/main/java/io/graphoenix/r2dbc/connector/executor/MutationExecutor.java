package io.graphoenix.r2dbc.connector.executor;

import com.google.common.collect.Lists;
import io.graphoenix.r2dbc.connector.connection.ConnectionCreator;
import io.r2dbc.spi.Batch;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MutationExecutor {

    private static final Logger log = LoggerFactory.getLogger(MutationExecutor.class);

    private final ConnectionCreator connectionCreator;

    @Inject
    public MutationExecutor(ConnectionCreator connectionCreator) {
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
                            .flatMap(this::getJsonStringFromResult)
                            .doOnError(throwable -> connection.rollbackTransaction())
                            .doFinally(signalType -> connection.close());
                });
    }

    public Stream<Integer> executeMutationsInBatchByGroup(Stream<String> sqlStream, int itemCount) {
        List<List<String>> sqlListGroup = Lists.partition(sqlStream.collect(Collectors.toList()), itemCount);

        return connectionCreator.createConnection()
                .map(connection -> {
                            connection.beginTransaction();
                            Stream<Integer> stream = sqlListGroup.stream()
                                    .map(sqlList -> {
                                        Batch batch = connection.createBatch();
                                        sqlList.forEach(batch::add);
                                        Mono.from(batch.execute())
                                                .doOnError(throwable -> {
                                                    throwable.printStackTrace();
                                                    connection.rollbackTransaction();
                                                    connection.close();
                                                })
                                                .block();
                                        return sqlList.size();
                                    });
                            connection.commitTransaction();
                            connection.close();
                            return stream;
                        }
                )
                .block();
    }

    public Mono<String> executeMutations(Stream<String> sqlStream) {
        return executeMutations(sqlStream, null);
    }

    public Mono<String> executeMutations(Stream<String> sqlStream, Map<String, Object> parameters) {
        return connectionCreator.createConnection()
                .flatMap(connection -> {
                    connection.beginTransaction();
                    return Flux.fromStream(sqlStream
                            .map(sql -> {
                                        Statement statement = connection.createStatement(sql);
                                        if (parameters != null) {
                                            parameters.forEach(statement::bind);
                                        }
                                        return statement;
                                    }
                            ))
                            .flatMap(Statement::execute)
                            .flatMap(this::getJsonStringFromResult)
                            .last()
                            .doOnSuccess(result -> connection.commitTransaction())
                            .doOnError(throwable -> connection.rollbackTransaction())
                            .doFinally(signalType -> connection.close());
                });
    }

    private Mono<String> getJsonStringFromResult(Result result) {
        return Mono.from(result.map((row, rowMetadata) -> row.get(0, String.class)));
    }
}
