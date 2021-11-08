package io.graphoenix.r2dbc.connector.executor;

import com.google.common.collect.Maps;
import io.graphoenix.r2dbc.connector.connection.IConnectionCreator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public class QueryExecutor {

    private final IConnectionCreator connectionCreator;

    public QueryExecutor(IConnectionCreator connectionCreator) {
        this.connectionCreator = connectionCreator;
    }

    public Mono<String> executeQuery(String sql) {
        return connectionCreator.createConnection()
                .flatMap(connection ->
                        Mono.from(connection.createStatement(sql).execute())
                                .doFinally(signalType -> connection.close())
                )
                .flatMap(result -> Mono.from(result.map((row, rowMetadata) -> row.get(0, String.class))));
    }


    public Flux<Map.Entry<String, String>> executeQuery(Stream<Map.Entry<String, String>> sqlStream) {
        return connectionCreator.createConnection()
                .flatMapMany(connection ->
                        Flux.fromStream(
                                sqlStream.map(
                                        sqlEntry ->
                                                Maps.immutableEntry(
                                                        sqlEntry.getKey(),
                                                        Objects.requireNonNull(
                                                                Mono.from(connection.createStatement(sqlEntry.getValue()).execute())
                                                                        .flatMap(result ->
                                                                                Mono.from(result.map((row, rowMetadata) -> row.get(0, String.class)))
                                                                        )
                                                                        .block()
                                                        )
                                                )
                                )
                        ).doFinally(signalType -> connection.close())
                );
    }
}
