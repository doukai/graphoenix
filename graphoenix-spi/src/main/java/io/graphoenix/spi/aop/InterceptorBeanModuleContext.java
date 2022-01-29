package io.graphoenix.spi.aop;

import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.function.Supplier;

public interface InterceptorBeanModuleContext {

    <T extends Annotation> Supplier<? extends Interceptor> get(Class<T> beanClass);

    <T extends Annotation> Optional<Supplier<? extends Interceptor>> getOptional(Class<T> beanClass);
}
