package io.graphoenix.gr2dbc.connector;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

public class QueryExecutor {

    private final ConnectionCreator connectionCreator;

    public QueryExecutor(ConnectionCreator connectionCreator) {
        this.connectionCreator = connectionCreator;
    }

    public Mono<String> executeQuery(String sql) {
        return connectionCreator.createConnection()
                .flatMap(connection -> Mono.from(connection.createStatement(sql).execute()).doFinally(signalType -> connection.close()))
                .flatMap(result -> Mono.from(result.map((row, rowMetadata) -> row.get(0, String.class))));
    }

    public Flux<SelectionResult> executeQueries(List<String> sqlList) {
        return connectionCreator.createConnection()
                .flatMapMany(connection -> Flux.concat(sqlList.stream().map(sql -> connection.createStatement(sql).execute()).collect(Collectors.toList())).doFinally(signalType -> connection.close()))
                .flatMap(result -> Mono.from(result.map((row, rowMetadata) -> new SelectionResult(rowMetadata.getColumnMetadata(0).getName(), row.get(0, String.class)))));
    }
}
