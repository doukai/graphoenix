package io.graphoenix.r2dbc.connector.handler.operation;

import io.graphoenix.r2dbc.connector.parameter.R2dbcParameterProcessor;
import io.graphoenix.spi.handler.IOperationHandler;
import io.graphoenix.spi.handler.IPipelineContext;

import java.util.Map;

public class R2dbcParameterProcessHandler implements IOperationHandler {
    @Override
    public void init(IPipelineContext context) throws Exception {

    }

    private void processParameters(IPipelineContext context) throws Exception {
        String sql = context.poll(String.class);
        R2dbcParameterProcessor processor = new R2dbcParameterProcessor();
        Map<String, Object> parameters = context.pollMap(String.class, Object.class);
        context.add(sql);
        context.add(processor.process(parameters));
    }

    @Override
    public boolean query(IPipelineContext context) throws Exception {
        processParameters(context);
        return false;
    }

    @Override
    public boolean queryAsync(IPipelineContext context) throws Exception {
        processParameters(context);
        return false;
    }

    @Override
    public boolean querySelectionsAsync(IPipelineContext context) throws Exception {
        processParameters(context);
        return false;
    }

    @Override
    public boolean mutation(IPipelineContext context) throws Exception {
        processParameters(context);
        return false;
    }

    @Override
    public boolean mutationAsync(IPipelineContext context) throws Exception {
        processParameters(context);
        return false;
    }

    @Override
    public boolean subscription(IPipelineContext context) throws Exception {
        processParameters(context);
        return false;
    }
}
