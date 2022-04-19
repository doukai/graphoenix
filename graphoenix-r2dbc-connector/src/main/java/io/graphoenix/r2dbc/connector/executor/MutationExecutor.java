package io.graphoenix.r2dbc.connector.executor;

import com.google.common.collect.Lists;
import io.graphoenix.r2dbc.connector.connection.ConnectionCreator;
import io.r2dbc.spi.Batch;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Statement;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.tinylog.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
public class MutationExecutor {

    private final ConnectionCreator connectionCreator;

    @Inject
    public MutationExecutor(ConnectionCreator connectionCreator) {
        this.connectionCreator = connectionCreator;
    }

    public Mono<String> executeMutationsInBatch(Stream<String> sqlStream) {

        return Flux
                .usingWhen(
                        connectionCreator.createConnection(),
                        connection -> {
                            connection.beginTransaction();
                            Batch batch = connection.createBatch();
                            sqlStream.forEach(sql -> {
                                        Logger.debug("execute statement:\r\n{}", sql);
                                        batch.add(sql);
                                    }
                            );
                            return batch.execute();
                        },
                        connection -> {
                            connection.commitTransaction();
                            return connection.close();
                        },
                        (connection, throwable) -> {
                            Logger.error(throwable);
                            connection.rollbackTransaction();
                            return connection.close();
                        },
                        connection -> {
                            connection.rollbackTransaction();
                            return connection.close();
                        }
                )
                .last()
                .flatMap(this::getJsonStringFromResult);
    }

    public Flux<Integer> executeMutationsInBatchByGroup(Stream<String> sqlStream, int itemCount) {
        List<List<String>> sqlListGroup = Lists.partition(sqlStream.collect(Collectors.toList()), itemCount);

        return Flux
                .usingWhen(
                        connectionCreator.createConnection(),
                        connection -> {
                            connection.beginTransaction();
                            return Flux.fromIterable(sqlListGroup)
                                    .map(sqlList -> {
                                                Batch batch = connection.createBatch();
                                                sqlList.forEach(sql -> {
                                                            Logger.debug("execute statement:\r\n{}", sql);
                                                            batch.add(sql);
                                                        }
                                                );
                                                return batch.execute();
                                            }
                                    );
                        },
                        connection -> {
                            connection.commitTransaction();
                            return connection.close();
                        },
                        (connection, throwable) -> {
                            Logger.error(throwable);
                            connection.rollbackTransaction();
                            return connection.close();
                        },
                        connection -> {
                            connection.rollbackTransaction();
                            return connection.close();
                        }
                )
                .flatMap(Mono::from)
                .flatMap(result -> Mono.from(result.getRowsUpdated()));
    }

    public Mono<String> executeMutations(Stream<String> sqlStream) {
        return executeMutations(sqlStream, null);
    }

    public Mono<String> executeMutations(Stream<String> sqlStream, Map<String, Object> parameters) {

        return Flux
                .usingWhen(
                        connectionCreator.createConnection(),
                        connection -> {
                            connection.beginTransaction();
                            return Flux.fromStream(
                                    sqlStream.map(sql -> {
                                                Logger.debug("execute statement:\r\n{}", sql);
                                                Logger.debug("parameters:\r\n{}", parameters);
                                                Statement statement = connection.createStatement(sql);
                                                if (parameters != null) {
                                                    parameters.forEach(statement::bind);
                                                }
                                                return statement;
                                            }
                                    )
                            ).flatMap(Statement::execute);
                        },
                        connection -> {
                            connection.commitTransaction();
                            return connection.close();
                        },
                        (connection, throwable) -> {
                            Logger.error(throwable);
                            connection.rollbackTransaction();
                            return connection.close();
                        },
                        connection -> {
                            connection.rollbackTransaction();
                            return connection.close();
                        }
                )
                .last()
                .flatMap(this::getJsonStringFromResult);
    }

    public Mono<String> executeMutations(String sql) {
        return executeMutations(sql, null);
    }

    public Mono<String> executeMutations(String sql, Map<String, Object> parameters) {
        return Flux
                .usingWhen(
                        connectionCreator.createConnection(),
                        connection -> {
                            connection.beginTransaction();
                            Logger.debug("execute statement:\r\n{}", sql);
                            Logger.debug("parameters:\r\n{}", parameters);
                            Statement statement = connection.createStatement(sql);
                            if (parameters != null) {
                                parameters.forEach(statement::bind);
                            }
                            return statement.execute();
                        },
                        connection -> {
                            connection.commitTransaction();
                            return connection.close();
                        },
                        (connection, throwable) -> {
                            Logger.error(throwable);
                            connection.rollbackTransaction();
                            return connection.close();
                        },
                        connection -> {
                            connection.rollbackTransaction();
                            return connection.close();
                        }
                )
                .last()
                .flatMap(this::getJsonStringFromResult);
    }

    private Mono<String> getJsonStringFromResult(Result result) {
        return Mono.from(result.map((row, rowMetadata) -> row.get(0, String.class)));
    }
}
