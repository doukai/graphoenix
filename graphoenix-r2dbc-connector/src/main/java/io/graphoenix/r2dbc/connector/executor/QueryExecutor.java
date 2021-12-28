package io.graphoenix.r2dbc.connector.executor;

import io.graphoenix.r2dbc.connector.connection.ConnectionCreator;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Statement;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.inject.Inject;
import java.util.Map;
import java.util.stream.Stream;

public class QueryExecutor {

    private final ConnectionCreator connectionCreator;

    @Inject
    public QueryExecutor(ConnectionCreator connectionCreator) {
        this.connectionCreator = connectionCreator;
    }

    public Mono<String> executeQuery(String sql, Map<String, Object> parameters) {
        return connectionCreator.createConnection()
                .flatMap(connection -> {
                            Statement statement = connection.createStatement(sql);
                            parameters.forEach(statement::bind);
                            return Mono.from(statement.execute())
                                    .doFinally(signalType -> connection.close());
                        }
                )
                .flatMap(this::getJsonStringFromResult);
    }

    public Flux<Tuple2<String, String>> executeQuery(Stream<Tuple2<String, String>> sqlStream, Map<String, Object> parameters) {
        return connectionCreator.createConnection()
                .flatMapMany(connection ->
                        Flux.fromStream(
                                sqlStream.map(
                                        tuple2 -> {
                                            Statement statement = connection.createStatement(tuple2._1());
                                            parameters.forEach(statement::bind);
                                            return Tuple.of(
                                                    tuple2._2(),
                                                    Mono.from(statement.execute())
                                                            .flatMap(this::getJsonStringFromResult)
                                                            .block()
                                            );
                                        }
                                )
                        ).doFinally(signalType -> connection.close())
                );
    }

    private Mono<String> getJsonStringFromResult(Result result) {
        return Mono.from(result.map((row, rowMetadata) -> row.get(0, String.class)));
    }
}
