package io.graphoenix.r2dbc.connector.handler.bootstrap;

import io.graphoenix.r2dbc.connector.executor.MutationExecutor;
import io.graphoenix.spi.handler.IBootstrapHandler;
import io.graphoenix.spi.handler.IPipelineContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.stream.Stream;

public class IntrospectionMutationExecuteHandler implements IBootstrapHandler {

    private static final Logger log = LoggerFactory.getLogger(IntrospectionMutationExecuteHandler.class);
    private static final int sqlCount = 500;

    private final MutationExecutor mutationExecutor;

    @Inject
    public IntrospectionMutationExecuteHandler(MutationExecutor mutationExecutor) {
        this.mutationExecutor = mutationExecutor;
    }

    @Override
    public boolean execute(IPipelineContext context) {
        Stream<String> sqlStream = context.pollStream(String.class);
        log.info("introspection data SQL insert started");
        mutationExecutor.executeMutationsInBatchByGroup(sqlStream, sqlCount).forEach(count -> log.info(count + " introspection data SQL insert success"));
        log.info("All introspection data SQL insert success");
        return false;
    }
}
