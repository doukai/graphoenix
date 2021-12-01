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
    private IGraphQLDocumentManager manager;

    private final SqlFormatter.Formatter formatter = SqlFormatter.of(Dialect.MariaDb).extend(cfg -> cfg.plusNamedPlaceholderTypes(":"));

    @Override
    public void setupManager(IGraphQLDocumentManager manager) {
        this.manager = manager;
    }

    @Override
    public Tuple2<String, String> query(Object sql) {
        return Tuples.of(formatter.format((String) sql), "sql");
    }

    @Override
    public Tuple2<String, String> queryAsync(Object sql) {
        return query(sql);
    }

    @Override
    public Tuple2<String, String> querySelectionsAsync(Object sql) {
        return query(sql);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Tuple2<String, String> mutation(Object sql) {
        return Tuples.of(formatter.format(((Stream<String>) sql).collect(Collectors.joining(";"))), "sql");
    }

    @Override
    public Tuple2<String, String> mutationAsync(Object sql) {
        return mutation(sql);
    }

    @Override
    public Tuple2<String, String> subscription(Object sql) {
        return query(sql);
    }
}
