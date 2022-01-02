package io.graphoenix.core.module;

import dagger.BindsOptionalOf;
import dagger.Module;
import io.graphoenix.spi.handler.BootstrapHandler;
import io.graphoenix.spi.handler.OperationHandler;

@Module
public interface HandlerModule {

    @BindsOptionalOf
    OperationHandler operationHandler();

    @BindsOptionalOf
    BootstrapHandler bootstrapHandler();
}
