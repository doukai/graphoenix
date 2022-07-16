package io.graphoenix.product.handler;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.mysql.handler.OperationToSQLConvertHandler;
import io.graphoenix.r2dbc.connector.handler.OperationSQLExecuteHandler;
import io.graphoenix.spi.handler.OperationHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import reactor.core.publisher.Mono;

import java.util.stream.Stream;

@ApplicationScoped
public class MysqlR2dbcHandler implements OperationHandler {

    private final OperationToSQLConvertHandler operationToSQLConvertHandler;
    private final OperationSQLExecuteHandler operationSQLExecuteHandler;

    @Inject
    public MysqlR2dbcHandler(OperationToSQLConvertHandler operationToSQLConvertHandler, OperationSQLExecuteHandler operationSQLExecuteHandler) {
        this.operationToSQLConvertHandler = operationToSQLConvertHandler;
        this.operationSQLExecuteHandler = operationSQLExecuteHandler;
    }

    @Override
    public Mono<String> query(GraphqlParser.OperationDefinitionContext operationDefinitionContext) {
        String select = operationToSQLConvertHandler.queryToSelect(operationDefinitionContext);
        return operationSQLExecuteHandler.query(select);
    }

    @Override
    public Mono<String> mutation(GraphqlParser.OperationDefinitionContext operationDefinitionContext) {
        Stream<String> statements = operationToSQLConvertHandler.mutationToStatements(operationDefinitionContext);
        return operationSQLExecuteHandler.mutation(statements);
    }
}
