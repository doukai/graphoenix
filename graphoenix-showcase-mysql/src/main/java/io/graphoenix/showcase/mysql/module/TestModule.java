package io.graphoenix.showcase.mysql.module;

import io.graphoenix.showcase.mysql.annotation.Aspect;
import io.graphoenix.showcase.mysql.aop.NotNullAop;
import io.graphoenix.showcase.mysql.bean.TestClass;
import io.graphoenix.spi.aop.InterceptorBean;
import io.graphoenix.spi.module.Module;
import io.graphoenix.spi.module.Provides;

import javax.inject.Singleton;

@Module
public class TestModule {

    @Provides
    @Singleton
    @InterceptorBean(Aspect.class)
    public NotNullAop notNullAop() {
        return new NotNullAop();
    }

    @Provides
    @Singleton
    public TestClass testClass() {
        return new TestClass();
    }
}
