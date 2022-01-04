package io.graphoenix.spi.handler;

public interface GeneratorHandler {

    String query(String graphQL);

    String mutation(String graphQL);

    String extension();
}
