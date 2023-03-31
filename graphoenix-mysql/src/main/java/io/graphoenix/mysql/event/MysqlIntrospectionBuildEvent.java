package io.graphoenix.mysql.event;

import com.google.auto.service.AutoService;
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.core.operation.Operation;
import io.graphoenix.graphql.builder.introspection.IntrospectionMutationBuilder;
import io.graphoenix.mysql.config.MysqlConfig;
import io.graphoenix.mysql.translator.translator.GraphQLMutationToStatements;
import io.graphoenix.mysql.translator.translator.GraphQLTypeToTable;
import io.graphoenix.r2dbc.connector.executor.MutationExecutor;
import io.graphoenix.spi.handler.ScopeEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import org.tinylog.Logger;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.stream.Collectors;

@Initialized(ApplicationScoped.class)
@Priority(2)
@AutoService(ScopeEvent.class)
public class MysqlIntrospectionBuildEvent implements ScopeEvent {

    private final MysqlConfig mysqlConfig;
    private final IntrospectionMutationBuilder introspectionMutationBuilder;
    private final GraphQLMutationToStatements mutationToStatements;
    private final GraphQLTypeToTable graphQLTypeToTable;
    private final MutationExecutor mutationExecutor;

    public MysqlIntrospectionBuildEvent() {
        this.mysqlConfig = BeanContext.get(MysqlConfig.class);
        this.introspectionMutationBuilder = BeanContext.get(IntrospectionMutationBuilder.class);
        this.mutationToStatements = BeanContext.get(GraphQLMutationToStatements.class);
        this.graphQLTypeToTable = BeanContext.get(GraphQLTypeToTable.class);
        this.mutationExecutor = BeanContext.get(MutationExecutor.class);
    }

    @Override
    public Mono<Void> fireAsync(Map<String, Object> context) {
        if (mysqlConfig.getCrateIntrospection()) {
            Logger.info("introspection data SQL insert started");
            Operation operation = introspectionMutationBuilder.buildIntrospectionSchemaMutation();
            return mutationExecutor.executeMutations(graphQLTypeToTable.truncateIntrospectionObjectTablesSQL().collect(Collectors.joining(";")))
                    .then(mutationExecutor.executeMutations(mutationToStatements.createStatementsSQL(operation.toString()).collect(Collectors.joining(";"))))
                    .then();
        }
        return Mono.empty();
    }
}
