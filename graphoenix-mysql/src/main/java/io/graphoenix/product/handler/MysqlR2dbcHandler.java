package io.graphoenix.product.handler;

import io.graphoenix.mysql.handler.operation.OperationToSQLConvertHandler;
import io.graphoenix.r2dbc.connector.handler.operation.OperationSQLExecuteHandler;
import io.graphoenix.spi.handler.OperationHandler;
import io.vavr.Tuple2;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.inject.Inject;
import java.util.Map;

public class MysqlR2dbcHandler implements OperationHandler {

    private final OperationToSQLConvertHandler operationToSQLConvertHandler;
    private final OperationSQLExecuteHandler operationSQLExecuteHandler;

    @Inject
    public MysqlR2dbcHandler(OperationToSQLConvertHandler operationToSQLConvertHandler, OperationSQLExecuteHandler operationSQLExecuteHandler) {
        this.operationToSQLConvertHandler = operationToSQLConvertHandler;
        this.operationSQLExecuteHandler = operationSQLExecuteHandler;
    }

    @Override
    public Mono<String> query(String graphQL, Map<String, Object> parameters) {
        return operationSQLExecuteHandler.query(operationToSQLConvertHandler.queryToSelect(graphQL), parameters);
    }

    @Override
    public Flux<Tuple2<String, String>> querySelections(String graphQL, Map<String, Object> parameters) {
        return operationSQLExecuteHandler.querySelections(operationToSQLConvertHandler.querySelectionsToSelects(graphQL), parameters);
    }

    @Override
    public Mono<String> mutation(String graphQL, Map<String, Object> parameters) {
        return operationSQLExecuteHandler.mutation(operationToSQLConvertHandler.mutationToStatements(graphQL), parameters);
    }
}
