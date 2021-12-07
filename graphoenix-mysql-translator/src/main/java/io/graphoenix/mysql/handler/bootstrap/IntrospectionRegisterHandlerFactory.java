package io.graphoenix.mysql.handler.bootstrap;

import dagger.Component;
import io.graphoenix.mysql.module.MySQLTranslatorModule;
import io.graphoenix.spi.handler.IBootstrapHandlerFactory;

import javax.inject.Singleton;

@Singleton
@Component(modules = MySQLTranslatorModule.class)
public interface IntrospectionRegisterHandlerFactory extends IBootstrapHandlerFactory {

    @Override
    IntrospectionRegisterHandler createHandler();
}
