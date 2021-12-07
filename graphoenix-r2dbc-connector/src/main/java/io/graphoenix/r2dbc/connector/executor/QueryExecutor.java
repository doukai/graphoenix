package io.graphoenix.r2dbc.connector.executor;

import io.graphoenix.r2dbc.connector.connection.IConnectionCreator;
import io.r2dbc.spi.Statement;
import org.javatuples.Pair;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.inject.Inject;
import java.util.Map;
import java.util.stream.Stream;

public class QueryExecutor {

    private final IConnectionCreator connectionCreator;

    @Inject
    public QueryExecutor(IConnectionCreator connectionCreator) {
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
                .flatMap(result -> Mono.from(result.map((row, rowMetadata) -> row.get(0, String.class))));
    }


    public Flux<Pair<String, String>> executeQuery(Stream<Pair<String, String>> sqlStream, Map<String, Object> parameters) {
        return connectionCreator.createConnection()
                .flatMapMany(connection ->
                        Flux.fromStream(
                                sqlStream.map(
                                        pair -> {
                                            Statement statement = connection.createStatement(pair.getValue1());
                                            parameters.forEach(statement::bind);
                                            return Pair.with(
                                                    pair.getValue0(),
                                                    Mono.from(statement.execute())
                                                            .flatMap(result ->
                                                                    Mono.from(result.map((row, rowMetadata) -> row.get(0, String.class)))
                                                            )
                                                            .block()
                                            );
                                        }
                                )
                        ).doFinally(signalType -> connection.close())
                );
    }
}
