package io.graphoenix.r2dbc.connector.handler.operation;

import io.graphoenix.r2dbc.connector.parameter.R2dbcParameterProcessor;
import io.graphoenix.spi.handler.IOperationHandler;
import io.graphoenix.r2dbc.connector.executor.MutationExecutor;
import io.graphoenix.r2dbc.connector.executor.QueryExecutor;
import io.graphoenix.spi.handler.IPipelineContext;
import org.javatuples.Pair;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.inject.Inject;
import java.util.Map;
import java.util.stream.Stream;

public class OperationSQLExecuteHandler implements IOperationHandler {

    private final QueryExecutor queryExecutor;

    private final MutationExecutor mutationExecutor;

    private final R2dbcParameterProcessor r2dbcParameterProcessor;

    @Inject
    public OperationSQLExecuteHandler(QueryExecutor queryExecutor, MutationExecutor mutationExecutor, R2dbcParameterProcessor r2dbcParameterProcessor) {
        this.queryExecutor = queryExecutor;
        this.mutationExecutor = mutationExecutor;
        this.r2dbcParameterProcessor = r2dbcParameterProcessor;
    }

    @Override
    public boolean query(IPipelineContext context) {
        String sql = context.poll(String.class);
        Map<String, Object> parameters = context.pollMap(String.class, Object.class);
        String result = queryExecutor.executeQuery(sql, r2dbcParameterProcessor.process(parameters)).block();
        context.add(result);
        return false;
    }

    @Override
    public boolean queryAsync(IPipelineContext context) {
        String sql = context.poll(String.class);
        Map<String, Object> parameters = context.pollMap(String.class, Object.class);
        Mono<String> result = queryExecutor.executeQuery(sql, r2dbcParameterProcessor.process(parameters));
        context.add(result);
        return false;
    }

    @Override
    public boolean querySelectionsAsync(IPipelineContext context) {
        Stream<Pair<String, String>> sqlStream = context.pollStreamPair(String.class, String.class);
        Map<String, Object> parameters = context.pollMap(String.class, Object.class);
        Flux<Pair<String, String>> result = queryExecutor.executeQuery(sqlStream, r2dbcParameterProcessor.process(parameters));
        context.add(result);
        return false;
    }

    @Override
    public boolean mutation(IPipelineContext context) {
        Stream<String> sqlStream = context.pollStream(String.class);
        Map<String, Object> parameters = context.pollMap(String.class, Object.class);
        String result = mutationExecutor.executeMutations(sqlStream, r2dbcParameterProcessor.process(parameters)).block();
        context.add(result);
        return false;
    }

    @Override
    public boolean mutationAsync(IPipelineContext context) {
        Stream<String> sqlStream = context.pollStream(String.class);
        Map<String, Object> parameters = context.pollMap(String.class, Object.class);
        Mono<String> result = mutationExecutor.executeMutations(sqlStream, r2dbcParameterProcessor.process(parameters));
        context.add(result);
        return false;
    }

    @Override
    public boolean subscription(IPipelineContext context) {
        //TODO
        return false;
    }
}
