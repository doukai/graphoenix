package io.graphoenix.product.event;

import com.google.auto.service.AutoService;
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.core.operation.Operation;
import io.graphoenix.graphql.builder.introspection.IntrospectionMutationBuilder;
import io.graphoenix.mysql.translator.GraphQLMutationToStatements;
import io.graphoenix.product.config.MysqlConfig;
import io.graphoenix.r2dbc.connector.executor.MutationExecutor;
import io.graphoenix.spi.handler.ScopeEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import org.tinylog.Logger;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.stream.Stream;

@Initialized(ApplicationScoped.class)
@Priority(2)
@AutoService(ScopeEvent.class)
public class MysqlIntrospectionBuildEvent implements ScopeEvent {

    private static final int sqlCount = 300;
    private final MysqlConfig mysqlConfig;
    private final IntrospectionMutationBuilder introspectionMutationBuilder;
    private final GraphQLMutationToStatements mutationToStatements;
    private final MutationExecutor mutationExecutor;

    public MysqlIntrospectionBuildEvent() {
        this.mysqlConfig = BeanContext.get(MysqlConfig.class);
        this.introspectionMutationBuilder = BeanContext.get(IntrospectionMutationBuilder.class);
        this.mutationToStatements = BeanContext.get(GraphQLMutationToStatements.class);
        this.mutationExecutor = BeanContext.get(MutationExecutor.class);
    }

    @Override
    public Mono<Void> fireAsync(Map<String, Object> context) {
        if (mysqlConfig.getCrateIntrospection()) {
            Logger.info("introspection data SQL insert started");
            Operation operation = introspectionMutationBuilder.buildIntrospectionSchemaMutation();
            Stream<String> introspectionMutationSQLStream = mutationToStatements.createStatementsSQL(operation.toString());
            return mutationExecutor.executeMutationsInBatchByGroup(introspectionMutationSQLStream, sqlCount)
                    .doOnNext(count -> Logger.info(count + " introspection data SQL insert success"))
                    .doOnComplete(() -> Logger.info("all introspection data SQL insert success"))
                    .reduce(0, Integer::sum)
                    .doOnSuccess(totalCount -> Logger.info("introspection insert total:\r\n{}", totalCount))
                    .then();
        }
        return Mono.empty();
    }
}
