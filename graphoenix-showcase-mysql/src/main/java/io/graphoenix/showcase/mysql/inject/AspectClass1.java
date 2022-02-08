package io.graphoenix.showcase.mysql.inject;

import io.graphoenix.showcase.mysql.annotation.Test;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@ApplicationScoped
@Test(value = "test")
@Priority(1)
@Interceptor
public class AspectClass1 {

    @AroundInvoke
    public Object processTest(InvocationContext invocationContext) {

        return null;
    }

    @AroundInvoke
    public Object processTest2(InvocationContext invocationContext) {

        return null;
    }

    @AroundConstruct
    public Object processTest3(InvocationContext invocationContext) {

        return null;
    }


    @AroundConstruct
    public Object processTest4(InvocationContext invocationContext) {

        return null;
    }
}
