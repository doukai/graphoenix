package io.graphoenix.mysql.event;

import com.google.auto.service.AutoService;
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.mysql.config.MysqlConfig;
import io.graphoenix.mysql.translator.translator.GraphQLTypeToTable;
import io.graphoenix.r2dbc.connector.executor.TableCreator;
import io.graphoenix.spi.handler.ScopeEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import org.tinylog.Logger;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.stream.Collectors;

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
            return tableCreator.selectColumns(graphqlTypeToTable.selectColumnsSQL())
                    .flatMap(existsColumnNameList -> tableCreator.mergeTable(graphqlTypeToTable.mergeTablesSQL(existsColumnNameList).collect(Collectors.joining(";"))))
                    .doOnSuccess((v) -> Logger.info("merge type table success"))
                    .doOnError(Logger::error)
                    .then();
        }
        return Mono.empty();
    }
}
