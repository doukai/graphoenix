package io.graphoenix.product.handler;

import io.graphoenix.mysql.handler.OperationToSQLConvertHandler;
import io.graphoenix.r2dbc.connector.handler.OperationSQLExecuteHandler;
import io.graphoenix.spi.handler.OperationHandler;
import io.vavr.Tuple2;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.inject.Inject;
import java.util.Map;
import java.util.stream.Stream;

public class MysqlR2dbcHandler implements OperationHandler {

    private final OperationToSQLConvertHandler operationToSQLConvertHandler;
    private final OperationSQLExecuteHandler operationSQLExecuteHandler;

    @Inject
    public MysqlR2dbcHandler(OperationToSQLConvertHandler operationToSQLConvertHandler, OperationSQLExecuteHandler operationSQLExecuteHandler) {
        this.operationToSQLConvertHandler = operationToSQLConvertHandler;
        this.operationSQLExecuteHandler = operationSQLExecuteHandler;
    }

    @Override
    public Mono<String> query(String graphQL, Map<String, String> variables) {
        String select = operationToSQLConvertHandler.queryToSelect(graphQL, variables);
        return operationSQLExecuteHandler.query(select);
    }

    @Override
    public Flux<Tuple2<String, String>> querySelections(String graphQL, Map<String, String> variables) {
        Stream<Tuple2<String, String>> selects = operationToSQLConvertHandler.querySelectionsToSelects(graphQL, variables);
        return operationSQLExecuteHandler.querySelections(selects);
    }

    @Override
    public Mono<String> mutation(String graphQL, Map<String, String> variables) {
        Stream<String> statements = operationToSQLConvertHandler.mutationToStatements(graphQL, variables);
        return operationSQLExecuteHandler.mutation(statements);
    }
}
