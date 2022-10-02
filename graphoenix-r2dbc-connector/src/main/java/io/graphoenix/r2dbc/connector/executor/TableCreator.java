package io.graphoenix.r2dbc.connector.executor;

import io.graphoenix.r2dbc.connector.connection.ConnectionCreator;
import io.r2dbc.spi.Batch;
import io.r2dbc.spi.Connection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.tinylog.Logger;
import reactor.core.publisher.Mono;

import java.util.stream.Stream;

@ApplicationScoped
public class TableCreator {

    private final ConnectionCreator connectionCreator;

    @Inject
    public TableCreator(ConnectionCreator connectionCreator) {
        this.connectionCreator = connectionCreator;
    }

    public Mono<Void> createTable(String sql) {
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

    public Mono<Void> createTables(Stream<String> sqlStream) {
        return Mono
                .usingWhen(
                        connectionCreator.createConnection(),
                        connection -> {
                            Batch batch = connection.createBatch();
                            sqlStream.forEach(sql -> {
                                        Logger.info("create table:\r\n{}", sql);
                                        batch.add(sql);
                                    }
                            );
                            return Mono.from(batch.execute());
                        },
                        Connection::close
                )
                .then();
    }
}
