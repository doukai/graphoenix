package io.graphoenix.r2dbc.connector.handler;

import io.graphoenix.r2dbc.connector.executor.MutationExecutor;
import io.graphoenix.r2dbc.connector.executor.QueryExecutor;
import io.graphoenix.r2dbc.connector.parameter.R2dbcParameterProcessor;
import io.vavr.Tuple2;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.stream.Stream;

@ApplicationScoped
public class OperationSQLExecuteHandler {

    private final QueryExecutor queryExecutor;

    private final MutationExecutor mutationExecutor;

    private final R2dbcParameterProcessor r2dbcParameterProcessor;

    @Inject
    public OperationSQLExecuteHandler(QueryExecutor queryExecutor, MutationExecutor mutationExecutor, R2dbcParameterProcessor r2dbcParameterProcessor) {
        this.queryExecutor = queryExecutor;
        this.mutationExecutor = mutationExecutor;
        this.r2dbcParameterProcessor = r2dbcParameterProcessor;
    }

    public Mono<String> query(String sql) {
        return queryExecutor.executeQuery(sql);
    }

    public Mono<String> query(String sql, Map<String, Object> parameters) {
        return queryExecutor.executeQuery(sql, r2dbcParameterProcessor.process(parameters));
    }

    public Flux<Tuple2<String, String>> querySelections(Stream<Tuple2<String, String>> sqlStream) {
        return queryExecutor.executeQuery(sqlStream);
    }

    public Flux<Tuple2<String, String>> querySelections(Stream<Tuple2<String, String>> sqlStream, Map<String, Object> parameters) {
        return queryExecutor.executeQuery(sqlStream, r2dbcParameterProcessor.process(parameters));
    }

    public Mono<String> mutation(Stream<String> sqlStream) {
        return mutationExecutor.executeMutations(sqlStream);
    }

    public Mono<String> mutation(Stream<String> sqlStream, Map<String, Object> parameters) {
        return mutationExecutor.executeMutations(sqlStream, r2dbcParameterProcessor.process(parameters));
    }
}
