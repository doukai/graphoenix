package io.graphoenix.r2dbc.connector.executor;

import io.graphoenix.r2dbc.connector.connection.ConnectionCreator;
import io.r2dbc.spi.Batch;
import io.r2dbc.spi.Connection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.tinylog.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.stream.Stream;

@ApplicationScoped
public class TableCreator {

    private final ConnectionCreator connectionCreator;

    @Inject
    public TableCreator(ConnectionCreator connectionCreator) {
        this.connectionCreator = connectionCreator;
    }

    public Mono<Integer> createTable(String sql) {

        return Flux
                .usingWhen(
                        connectionCreator.createConnection(),
                        connection -> {
                            Logger.debug("execute select:\r\n{}", sql);
                            return connection.createStatement(sql).execute();
                        },
                        Connection::close
                )
                .last()
                .flatMap(result -> Mono.from(result.getRowsUpdated()));
    }

    public Mono<Integer> createTables(Stream<String> sqlStream) {

        return Flux
                .usingWhen(
                        connectionCreator.createConnection(),
                        connection -> {
                            Batch batch = connection.createBatch();
                            sqlStream.forEach(sql -> {
                                        Logger.debug("create table:\r\n{}", sql);
                                        batch.add(sql);
                                    }
                            );
                            return batch.execute();
                        },
                        Connection::close
                )
                .last()
                .flatMap(result -> Mono.from(result.getRowsUpdated()));
    }
}
