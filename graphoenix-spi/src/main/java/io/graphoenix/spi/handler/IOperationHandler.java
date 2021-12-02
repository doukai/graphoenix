package io.graphoenix.spi.handler;

public interface IOperationHandler {

    void init(IPipelineContext context) throws Exception;

    void query(IPipelineContext context) throws Exception;

    void queryAsync(IPipelineContext context) throws Exception;

    void querySelectionsAsync(IPipelineContext context) throws Exception;

    void mutation(IPipelineContext context) throws Exception;

    void mutationAsync(IPipelineContext context) throws Exception;

    void subscription(IPipelineContext context) throws Exception;
}
