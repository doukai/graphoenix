package io.graphoenix.spi.handler;

public interface IOperationHandler {

    void init(IPipelineContext context) throws Exception;

    boolean query(IPipelineContext context) throws Exception;

    boolean queryAsync(IPipelineContext context) throws Exception;

    boolean querySelectionsAsync(IPipelineContext context) throws Exception;

    boolean mutation(IPipelineContext context) throws Exception;

    boolean mutationAsync(IPipelineContext context) throws Exception;

    boolean subscription(IPipelineContext context) throws Exception;
}
