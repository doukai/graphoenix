package io.graphoenix.spi.annotation;

import io.graphoenix.spi.handler.IBootstrapHandler;
import io.graphoenix.spi.handler.IOperationHandler;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface GraphQLOperation {

    Class<? extends IBootstrapHandler>[] bootstrapHandlers() default {};

    Class<? extends IOperationHandler>[] operationHandlers() default {};
}
