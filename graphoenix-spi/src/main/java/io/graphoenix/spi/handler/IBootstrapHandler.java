package io.graphoenix.spi.handler;

public interface IBootstrapHandler {

    boolean execute(IPipelineContext context) throws Exception;
}
