package io.graphoenix.showcase.mysql.inject;

import io.graphoenix.showcase.mysql.annotation.Test;
import io.graphoenix.spi.aop.InvocationContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;

@ApplicationScoped
@Test(value = "test")
@Interceptor
public class AspectClass1 {

    @AroundInvoke
    private Object processTest(InvocationContext invocationContext) {

        return null;
    }

    @AroundInvoke
    private Object processTest2(InvocationContext invocationContext) {

        return null;
    }
}
