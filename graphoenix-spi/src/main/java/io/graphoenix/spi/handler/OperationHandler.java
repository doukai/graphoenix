package io.graphoenix.spi.handler;

import io.vavr.Tuple2;
import org.reactivestreams.Publisher;

import java.util.Map;

public interface OperationHandler {

    Publisher<String> query(String graphQL, Map<String, Object> parameters);

    Publisher<Tuple2<String, String>> querySelections(String graphQL, Map<String, Object> parameters);

    Publisher<String> mutation(String graphQL, Map<String, Object> parameters);
}
