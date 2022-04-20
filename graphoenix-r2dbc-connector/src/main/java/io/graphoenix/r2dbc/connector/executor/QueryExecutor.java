package io.graphoenix.r2dbc.connector.executor;

import io.graphoenix.r2dbc.connector.connection.ConnectionCreator;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Statement;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.tinylog.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.stream.Stream;

@ApplicationScoped
public class QueryExecutor {

    private final ConnectionCreator connectionCreator;

    @Inject
    public QueryExecutor(ConnectionCreator connectionCreator) {
        this.connectionCreator = connectionCreator;
    }

    public Mono<String> executeQuery(String sql) {
        return executeQuery(sql, null);
    }

    public Mono<String> executeQuery(String sql, Map<String, Object> parameters) {

        return Flux
                .usingWhen(
                        connectionCreator.createConnection(),
                        connection -> {
                            Logger.debug("execute select:\r\n{}", sql);
                            Logger.debug("parameters:\r\n{}", parameters);
                            Statement statement = connection.createStatement(sql);
                            if (parameters != null) {
                                parameters.forEach(statement::bind);
                            }
                            return statement.execute();
                        },
                        Connection::close
                )
                .single()
                .flatMap(this::getJsonStringFromResult);
    }

    public Flux<Tuple2<String, String>> executeQuery(Stream<Tuple2<String, String>> sqlStream) {
        return executeQuery(sqlStream, null);
    }

    public Flux<Tuple2<String, String>> executeQuery(Stream<Tuple2<String, String>> sqlStream, Map<String, Object> parameters) {

        return Flux
                .usingWhen(
                        connectionCreator.createConnection(),
                        connection ->
                                Flux.fromStream(sqlStream)
                                        .flatMap(tuple2 -> {
                                                    String sql = tuple2._2();
                                                    Logger.debug("execute select:\r\n{}", sql);
                                                    Logger.debug("parameters:\r\n{}", parameters);
                                                    Statement statement = connection.createStatement(sql);
                                                    if (parameters != null) {
                                                        parameters.forEach(statement::bind);
                                                    }
                                                    return Mono.from(statement.execute())
                                                            .flatMap(this::getJsonStringFromResult)
                                                            .map(jsonString -> Tuple.of(tuple2._1(), jsonString));
                                                }
                                        ),
                        Connection::close
                );
    }

    private Mono<String> getJsonStringFromResult(Result result) {
        return Mono.from(result.map((row, rowMetadata) -> row.get(0, String.class)));
    }
}
