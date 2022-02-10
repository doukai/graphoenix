package io.graphoenix.product.handler;

import io.graphoenix.mysql.handler.OperationToSQLConvertHandler;
import io.graphoenix.mysql.handler.SQLFormatHandler;
import io.graphoenix.spi.handler.GeneratorHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.stream.Stream;

@ApplicationScoped
public class MysqlGeneratorHandler implements GeneratorHandler {

    private final OperationToSQLConvertHandler operationToSQLConvertHandler;
    private final SQLFormatHandler sqlFormatHandler;

    @Inject
    public MysqlGeneratorHandler(OperationToSQLConvertHandler operationToSQLConvertHandler, SQLFormatHandler sqlFormatHandler) {
        this.operationToSQLConvertHandler = operationToSQLConvertHandler;
        this.sqlFormatHandler = sqlFormatHandler;
    }

    @Override
    public String query(String graphQL) {
        String select = operationToSQLConvertHandler.queryToSelect(graphQL);
        return sqlFormatHandler.query(select);
    }

    @Override
    public String mutation(String graphQL) {
        Stream<String> statements = operationToSQLConvertHandler.mutationToStatements(graphQL);
        return sqlFormatHandler.mutation(statements);
    }

    @Override
    public String extension() {
        return "sql";
    }
}
