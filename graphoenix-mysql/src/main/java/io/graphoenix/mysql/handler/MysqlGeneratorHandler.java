package io.graphoenix.mysql.handler;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.mysql.translator.handler.OperationToSQLConvertHandler;
import io.graphoenix.mysql.translator.handler.SQLFormatHandler;
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
    public String query(GraphqlParser.OperationDefinitionContext operationDefinitionContext) {
        String select = operationToSQLConvertHandler.queryToSelect(operationDefinitionContext);
        return sqlFormatHandler.query(select);
    }

    @Override
    public String mutation(GraphqlParser.OperationDefinitionContext operationDefinitionContext) {
        Stream<String> statements = operationToSQLConvertHandler.mutationToStatements(operationDefinitionContext);
        return sqlFormatHandler.mutation(statements);
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
