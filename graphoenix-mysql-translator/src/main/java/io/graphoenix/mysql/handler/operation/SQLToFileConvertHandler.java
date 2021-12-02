package io.graphoenix.mysql.handler.operation;

import com.github.vertical_blank.sqlformatter.SqlFormatter;
import com.github.vertical_blank.sqlformatter.languages.Dialect;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.handler.IOperationHandler;
import io.graphoenix.spi.handler.IPipelineContext;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SQLToFileConvertHandler implements IOperationHandler {
    private IGraphQLDocumentManager manager;

    private final SqlFormatter.Formatter formatter = SqlFormatter.of(Dialect.MariaDb).extend(cfg -> cfg.plusNamedPlaceholderTypes(":"));

    @Override
    public void init(IPipelineContext context) {
        this.manager = context.getManager();
    }

    @Override
    public void query(IPipelineContext context) {
        String sql = context.poll(String.class);
        context.add(formatter.format(sql));
        context.add("sql");
    }

    @Override
    public void queryAsync(IPipelineContext context) {
        query(context);
    }

    @Override
    public void querySelectionsAsync(IPipelineContext context) {
        query(context);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void mutation(IPipelineContext context) {
        Stream<String> sqlStream = context.poll(Stream.class);
        context.add(formatter.format(sqlStream.collect(Collectors.joining(";"))));
        context.add("sql");
    }

    @Override
    public void mutationAsync(IPipelineContext context) {
        mutation(context);
    }

    @Override
    public void subscription(IPipelineContext context) {
        //TODO
    }
}
