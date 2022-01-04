package io.graphoenix.spi.annotation;

import io.graphoenix.spi.handler.IOperationHandler;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface GraphQLOperation {

    Class<? extends IOperationHandler> executeHandler();

    boolean useInject() default false;
}
