package io.graphoenix.spi.aop;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public abstract class BaseInterceptorBeanModuleContext implements InterceptorBeanModuleContext {

    private static final Map<Class<? extends Annotation>, Supplier<? extends Interceptor>> contextMap = new HashMap<>();

    protected static void put(Class<? extends Annotation> beanClass, Supplier<? extends Interceptor> supplier) {
        contextMap.put(beanClass, supplier);
    }

    @Override
    public <T extends Annotation> Supplier<? extends Interceptor> get(Class<T> beanClass) {
        return contextMap.get(beanClass);
    }

    @Override
    public <T extends Annotation> Optional<Supplier<? extends Interceptor>> getOptional(Class<T> beanClass) {
        if (contextMap.get(beanClass) != null) {
            return Optional.of(contextMap.get(beanClass));
        }
        return Optional.empty();
    }
}
