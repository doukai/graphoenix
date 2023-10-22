package io.graphoenix.r2dbc.connector.executor;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.Statement;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.tinylog.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.stream.Stream;

import static io.graphoenix.r2dbc.connector.utils.ResultUtil.RESULT_UTIL;
import static io.graphoenix.spi.constant.Hammurabi.TRANSACTION_ID;
import static io.graphoenix.spi.constant.Hammurabi.TRANSACTION_TYPE;
import static io.graphoenix.spi.constant.Hammurabi.TransactionType.IN_TRANSACTION;

@ApplicationScoped
public class QueryExecutor {

    private final Provider<Mono<Connection>> connectionMonoProvider;

    @Inject
    public QueryExecutor(Provider<Mono<Connection>> connectionMonoProvider) {
        this.connectionMonoProvider = connectionMonoProvider;
    }

    public Mono<String> executeQuery(String sql) {
        return executeQuery(sql, null);
    }

    public Mono<String> executeQuery(String sql, Map<String, Object> parameters) {
        return Mono
                .usingWhen(
                        connectionMonoProvider.get(),
                        connection -> {
                            Logger.info("execute select:\r\n{}", sql);
                            Logger.info("sql parameters:\r\n{}", parameters);
                            Statement statement = connection.createStatement(sql);
                            if (parameters != null) {
                                parameters.forEach(statement::bind);
                            }
                            return Mono.from(statement.execute());
                        },
                        connection -> Mono.deferContextual(contextView -> Mono.just(contextView.hasKey(TRANSACTION_ID) && contextView.hasKey(TRANSACTION_TYPE) && contextView.get(TRANSACTION_TYPE).equals(IN_TRANSACTION)))
                                .filter(inTransaction -> !inTransaction)
                                .flatMap(inTransaction -> Mono.from(connection.close()))
                )
                .flatMap(RESULT_UTIL::getJsonStringFromResult);
    }

    public Flux<Tuple2<String, String>> executeQuery(Stream<Tuple2<String, String>> sqlStream) {
        return executeQuery(sqlStream, null);
    }

    public Flux<Tuple2<String, String>> executeQuery(Stream<Tuple2<String, String>> sqlStream, Map<String, Object> parameters) {
        return Flux
                .usingWhen(
                        connectionMonoProvider.get(),
                        connection ->
                                Flux.fromStream(sqlStream)
                                        .flatMap(tuple2 -> {
                                                    String sql = tuple2._2();
                                                    Logger.info("execute select:\r\n{}", sql);
                                                    Logger.info("sql parameters:\r\n{}", parameters);
                                                    Statement statement = connection.createStatement(sql);
                                                    if (parameters != null) {
                                                        parameters.forEach(statement::bind);
                                                    }
                                                    return Mono.from(statement.execute())
                                                            .flatMap(RESULT_UTIL::getJsonStringFromResult)
                                                            .map(jsonString -> Tuple.of(tuple2._1(), jsonString));
                                                }
                                        ),
                        connection -> Mono.deferContextual(contextView -> Mono.just(contextView.hasKey(TRANSACTION_ID) && contextView.hasKey(TRANSACTION_TYPE) && contextView.get(TRANSACTION_TYPE).equals(IN_TRANSACTION)))
                                .filter(inTransaction -> !inTransaction)
                                .flatMap(inTransaction -> Mono.from(connection.close()))
                );
    }
}
