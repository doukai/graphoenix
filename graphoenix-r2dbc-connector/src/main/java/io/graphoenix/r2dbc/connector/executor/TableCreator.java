package io.graphoenix.r2dbc.connector.executor;

import io.graphoenix.r2dbc.connector.connection.ConnectionCreator;
import io.r2dbc.spi.Batch;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.Statement;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.tinylog.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Stream;

@ApplicationScoped
public class TableCreator {

    private final ConnectionCreator connectionCreator;

    @Inject
    public TableCreator(ConnectionCreator connectionCreator) {
        this.connectionCreator = connectionCreator;
    }

    public Mono<Void> mergeTable(String sql) {
        return Mono
                .usingWhen(
                        connectionCreator.createConnection(),
                        connection -> {
                            Logger.info("create table:\r\n{}", sql);
                            return Mono.from(connection.createStatement(sql).execute());
                        },
                        Connection::close
                )
                .then();
    }

    public Mono<Void> mergeTable(Stream<String> sqlStream) {
        return Flux
                .usingWhen(
                        connectionCreator.createConnection(),
                        connection -> {
                            Batch batch = connection.createBatch();
                            sqlStream.forEach(sql -> {
                                        Logger.info("create table:\r\n{}", sql);
                                        batch.add(sql);
                                    }
                            );
                            return Flux.from(batch.execute());
                        },
                        Connection::close
                )
                .then();
    }

    public Mono<List<Tuple2<String, String>>> selectColumns(String sql) {
        return Flux
                .usingWhen(
                        connectionCreator.createConnection(),
                        connection -> {
                            Logger.info("execute select:\r\n{}", sql);
                            Logger.info("sql parameters:\r\n{}");
                            Statement statement = connection.createStatement(sql);
                            return Flux.from(statement.execute());
                        },
                        Connection::close
                )
                .flatMap(result -> Flux.from(result.map((row, rowMetadata) -> Tuple.of(row.get(0, String.class), row.get(1, String.class)))))
                .collectList();
    }
}
