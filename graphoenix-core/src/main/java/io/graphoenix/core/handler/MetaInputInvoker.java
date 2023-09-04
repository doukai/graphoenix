package io.graphoenix.core.handler;

import io.graphoenix.core.dto.inputObjectType.MetaInput;
import reactor.core.publisher.Mono;

public interface MetaInputInvoker {

    Mono<MetaInput> invoke(String typeName, MetaInput metaInput);
}
