package io.graphoenix.r2dbc.connector.executor;

import com.google.common.collect.Lists;
import io.r2dbc.spi.Batch;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Statement;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.transaction.TransactionScoped;
import org.tinylog.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
public class MutationExecutor {

    private final Provider<Mono<Connection>> connectionMonoProvider;

    @Inject
    public MutationExecutor(Provider<Mono<Connection>> connectionMonoProvider) {
        this.connectionMonoProvider = connectionMonoProvider;
    }

    @TransactionScoped
    public Mono<String> executeMutationsInBatch(Stream<String> sqlStream) {
        return this.connectionMonoProvider.get()
                .flatMapMany(connection -> {
                            Batch batch = connection.createBatch();
                            sqlStream.forEach(sql -> {
                                        Logger.info("execute statement:\r\n{}", sql);
                                        batch.add(sql);
                                    }
                            );
                            return batch.execute();
                        }
                )
                .last()
                .flatMap(this::getJsonStringFromResult);
    }

    @TransactionScoped
    public Flux<Integer> executeMutationsInBatchByGroup(Stream<String> sqlStream, int itemCount) {
        List<List<String>> sqlListGroup = Lists.partition(sqlStream.collect(Collectors.toList()), itemCount);
        return this.connectionMonoProvider.get()
                .flatMapMany(connection ->
                        Flux.fromIterable(sqlListGroup)
                                .flatMap(sqlList -> {
                                            Batch batch = connection.createBatch();
                                            Logger.info("execute statement count:\r\n{}", sqlList.size());
                                            sqlList.forEach(batch::add);
                                            return Mono.from(batch.execute()).thenReturn(sqlList.size());
                                        }
                                )
                );
    }

    public Mono<String> executeMutations(Stream<String> sqlStream) {
        return executeMutations(sqlStream, null);
    }

    @TransactionScoped
    public Mono<String> executeMutations(Stream<String> sqlStream, Map<String, Object> parameters) {
        return this.connectionMonoProvider.get()
                .flatMapMany(connection ->
                        Flux.fromStream(sqlStream)
                                .map(sql -> {
                                            Logger.info("execute statement:\r\n{}", sql);
                                            Logger.info("sql parameters:\r\n{}", parameters);
                                            Statement statement = connection.createStatement(sql);
                                            if (parameters != null) {
                                                parameters.forEach(statement::bind);
                                            }
                                            return statement;
                                        }
                                )
                                .flatMap(Statement::execute)
                )
                .last()
                .flatMap(this::getJsonStringFromResult);
    }

    public Mono<String> executeMutations(String sql) {
        return executeMutations(sql, null);
    }

    @TransactionScoped
    public Mono<String> executeMutations(String sql, Map<String, Object> parameters) {
        return this.connectionMonoProvider.get()
                .flatMapMany(connection -> {
                            Logger.info("execute statement:\r\n{}", sql);
                            Logger.info("sql parameters:\r\n{}", parameters);
                            Statement statement = connection.createStatement(sql);
                            if (parameters != null) {
                                parameters.forEach(statement::bind);
                            }
                            return statement.execute();
                        }
                )
                .last()
                .flatMap(this::getJsonStringFromResult);
    }

    private Mono<String> getJsonStringFromResult(Result result) {
        return Mono.from(result.map((row, rowMetadata) -> row.get(0, String.class)));
    }
}
