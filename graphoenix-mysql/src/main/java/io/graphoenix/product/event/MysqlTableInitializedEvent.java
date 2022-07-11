package io.graphoenix.product.event;

import com.google.auto.service.AutoService;
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.mysql.translator.GraphQLTypeToTable;
import io.graphoenix.product.config.MysqlConfig;
import io.graphoenix.r2dbc.connector.executor.TableCreator;
import io.graphoenix.spi.handler.ScopeEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import org.tinylog.Logger;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.stream.Stream;

@Initialized(ApplicationScoped.class)
@Priority(1)
@AutoService(ScopeEvent.class)
public class MysqlTableInitializedEvent implements ScopeEvent {

    private final MysqlConfig mysqlConfig;
    private final GraphQLTypeToTable graphqlTypeToTable;
    private final TableCreator tableCreator;

    public MysqlTableInitializedEvent() {
        this.mysqlConfig = BeanContext.get(MysqlConfig.class);
        this.graphqlTypeToTable = BeanContext.get(GraphQLTypeToTable.class);
        this.tableCreator = BeanContext.get(TableCreator.class);
    }

    @Override
    public Mono<Void> fireAsync(Map<String, Object> context) {
        if (mysqlConfig.getCrateTable()) {
            Logger.info("start create type table");
            Stream<String> createTablesSQLStream = graphqlTypeToTable.createTablesSQL();
            return tableCreator.createTables(createTablesSQLStream).doOnSuccess((v) -> Logger.info("create type table success"));
        }
        return Mono.empty();
    }
}
