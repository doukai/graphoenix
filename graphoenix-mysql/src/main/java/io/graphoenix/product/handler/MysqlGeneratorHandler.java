package io.graphoenix.product.handler;

import io.graphoenix.mysql.handler.OperationToSQLConvertHandler;
import io.graphoenix.mysql.handler.SQLFormatHandler;
import io.graphoenix.spi.handler.GeneratorHandler;

import javax.inject.Inject;
import java.util.Map;
import java.util.stream.Stream;

public class MysqlGeneratorHandler implements GeneratorHandler {

    private final OperationToSQLConvertHandler operationToSQLConvertHandler;
    private final SQLFormatHandler sqlFormatHandler;

    @Inject
    public MysqlGeneratorHandler(OperationToSQLConvertHandler operationToSQLConvertHandler, SQLFormatHandler sqlFormatHandler) {
        this.operationToSQLConvertHandler = operationToSQLConvertHandler;
        this.sqlFormatHandler = sqlFormatHandler;
    }

    @Override
    public String query(String graphQL, Map<String, String> variables) {
        String select = operationToSQLConvertHandler.queryToSelect(graphQL);
        return sqlFormatHandler.query(select);
    }

    @Override
    public String mutation(String graphQL, Map<String, String> variables) {
        Stream<String> statements = operationToSQLConvertHandler.mutationToStatements(graphQL);
        return sqlFormatHandler.mutation(statements);
    }

    @Override
    public String extension() {
        return "sql";
    }
}
