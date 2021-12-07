package io.graphoenix.mysql.handler.operation;

import com.github.vertical_blank.sqlformatter.SqlFormatter;
import com.github.vertical_blank.sqlformatter.languages.Dialect;
import io.graphoenix.spi.handler.IOperationHandler;
import io.graphoenix.spi.handler.IPipelineContext;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SQLToFileConvertHandler implements IOperationHandler {

    private final SqlFormatter.Formatter formatter = SqlFormatter.of(Dialect.MariaDb).extend(cfg -> cfg.plusNamedPlaceholderTypes(":"));

    @Override
    public void init(IPipelineContext context) {
    }

    @Override
    public boolean query(IPipelineContext context) {
        String sql = context.poll(String.class);
        context.add(formatter.format(sql));
        return false;
    }

    @Override
    public boolean queryAsync(IPipelineContext context) {
        query(context);
        return false;
    }

    @Override
    public boolean querySelectionsAsync(IPipelineContext context) {
        query(context);
        return false;
    }

    @Override
    public boolean mutation(IPipelineContext context) {
        Stream<String> sqlStream = context.pollStream(String.class);
        context.add(formatter.format(sqlStream.collect(Collectors.joining(";"))));
        return false;
    }

    @Override
    public boolean mutationAsync(IPipelineContext context) {
        mutation(context);
        return false;
    }

    @Override
    public boolean subscription(IPipelineContext context) {
        //TODO
        return false;
    }
}
