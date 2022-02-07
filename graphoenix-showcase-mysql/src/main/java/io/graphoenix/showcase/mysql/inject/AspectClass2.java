package io.graphoenix.showcase.mysql.inject;

import io.graphoenix.showcase.mysql.annotation.Test2;
import io.graphoenix.spi.aop.InvocationContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;

@ApplicationScoped
@Test2(value = "test2")
@Interceptor
public class AspectClass2 {

    @AroundInvoke
    private Object processTest2(InvocationContext invocationContext) {

        return null;
    }

    @AroundInvoke
    private Object processTest22(InvocationContext invocationContext) {

        return null;
    }
}
