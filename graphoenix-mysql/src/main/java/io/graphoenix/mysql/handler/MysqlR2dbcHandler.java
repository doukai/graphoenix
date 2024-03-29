package io.graphoenix.mysql.handler;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.mysql.translator.handler.OperationToSQLConvertHandler;
import io.graphoenix.r2dbc.connector.handler.OperationSQLExecuteHandler;
import io.graphoenix.spi.handler.OperationHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.stream.Stream;

@ApplicationScoped
@Named("mysql")
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
        return Mono.justOrEmpty(operationToSQLConvertHandler.queryToSelect(operationDefinitionContext))
                .flatMap(operationSQLExecuteHandler::query);
    }

    @Override
    public Mono<String> mutation(GraphqlParser.OperationDefinitionContext operationDefinitionContext) {
        Stream<String> statements = operationToSQLConvertHandler.mutationToStatements(operationDefinitionContext);
        return operationSQLExecuteHandler.mutation(statements);
    }
}
