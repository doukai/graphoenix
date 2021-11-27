package io.graphoenix.mysql.handler.operation;

import com.github.vertical_blank.sqlformatter.SqlFormatter;
import com.github.vertical_blank.sqlformatter.languages.Dialect;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.handler.IOperationHandler;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SQLToFileConvertHandler implements IOperationHandler {

    @Override
    public void setupManager(IGraphQLDocumentManager manager) {
    }

    @Override
    public Tuple2<String, String> query(Object graphQL) {
        return Tuples.of(SqlFormatter.of(Dialect.MySql).format((String) graphQL), "sql");
    }

    @Override
    public Tuple2<String, String> queryAsync(Object graphQL) {
        return query(graphQL);
    }

    @Override
    public Tuple2<String, String> querySelectionsAsync(Object graphQL) {
        return query(graphQL);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Tuple2<String, String> mutation(Object graphQL) {
        return Tuples.of(SqlFormatter.of(Dialect.MySql).format(((Stream<String>) graphQL).collect(Collectors.joining(";"))), "sql");
    }

    @Override
    public Tuple2<String, String> mutationAsync(Object graphQL) {
        return mutationAsync(graphQL);
    }

    @Override
    public Tuple2<String, String> subscription(Object graphQL) {
        return null;
    }
}
