package io.graphoenix.spi.aop;

public interface Interceptor {

    boolean before(InvocationContext context);

    void after(InvocationContext context);
}
