package io.graphoenix.http.server;

import io.graphoenix.spi.aop.Interceptor;
import io.graphoenix.spi.aop.InvocationContext;

public class TestAop implements Interceptor {
    @Override
    public boolean before(InvocationContext context) {
        return false;
    }

    @Override
    public void after(InvocationContext context) {

    }
}
