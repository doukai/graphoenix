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
        return this.connectionMonoProvider.get()
                .flatMap(connection -> {
                            Logger.info("execute select:\r\n{}", sql);
                            Logger.info("sql parameters:\r\n{}", parameters);
                            Statement statement = connection.createStatement(sql);
                            if (parameters != null) {
                                parameters.forEach(statement::bind);
                            }
                            return Mono.from(statement.execute());
                        }
                )
                .flatMap(RESULT_UTIL::getJsonStringFromResult);
    }

    public Flux<Tuple2<String, String>> executeQuery(Stream<Tuple2<String, String>> sqlStream) {
        return executeQuery(sqlStream, null);
    }

    public Flux<Tuple2<String, String>> executeQuery(Stream<Tuple2<String, String>> sqlStream, Map<String, Object> parameters) {
        return this.connectionMonoProvider.get()
                .flatMapMany(connection ->
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
                                )
                );
    }
}
