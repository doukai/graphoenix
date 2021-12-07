package io.graphoenix.mysql.handler.operation;

import dagger.Component;
import io.graphoenix.mysql.module.MySQLTranslatorModule;
import io.graphoenix.spi.handler.IOperationHandlerFactory;

import javax.inject.Singleton;

@Singleton
@Component(modules = MySQLTranslatorModule.class)
public interface SQLToFileConvertHandlerFactory extends IOperationHandlerFactory {

    @Override
    SQLToFileConvertHandler createHandler();
}
