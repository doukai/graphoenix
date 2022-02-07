package io.graphoenix.showcase.mysql.inject;

import io.graphoenix.showcase.mysql.annotation.Test;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@ApplicationScoped
@Test(value = "test")
@Priority(0)
@Interceptor
public class AspectClass2 {

    @AroundInvoke
    public Object processTest2(InvocationContext invocationContext) {

        return null;
    }

    @AroundInvoke
    public Object processTest22(InvocationContext invocationContext) {

        return null;
    }
}
