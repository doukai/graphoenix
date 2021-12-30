package io.graphoenix.r2dbc.connector.handler.bootstrap;

import io.graphoenix.r2dbc.connector.executor.MutationExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.stream.Stream;

public class IntrospectionMutationExecuteHandler {

    private static final Logger log = LoggerFactory.getLogger(IntrospectionMutationExecuteHandler.class);
    private static final int sqlCount = 500;

    private final MutationExecutor mutationExecutor;

    @Inject
    public IntrospectionMutationExecuteHandler(MutationExecutor mutationExecutor) {
        this.mutationExecutor = mutationExecutor;
    }

    public boolean execute(Stream<String> sqlStream) {
        log.info("introspection data SQL insert started");
        mutationExecutor.executeMutationsInBatchByGroup(sqlStream, sqlCount).forEach(count -> log.info(count + " introspection data SQL insert success"));
        log.info("All introspection data SQL insert success");
        return false;
    }
}
