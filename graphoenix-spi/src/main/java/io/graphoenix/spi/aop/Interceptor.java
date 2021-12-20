package io.graphoenix.spi.aop;

public interface Interceptor {

    void before(InvocationContext context);

    void after(InvocationContext context);
}
