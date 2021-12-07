package io.graphoenix.mysql.handler.operation;

import dagger.BindsInstance;
import dagger.Component;
import io.graphoenix.mysql.module.MySQLTranslatorModule;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.antlr.IGraphQLFieldMapManager;
import io.graphoenix.spi.handler.IOperationHandlerFactory;

import javax.inject.Singleton;

@Singleton
@Component(modules = MySQLTranslatorModule.class)
public interface OperationToSQLConvertHandlerFactory extends IOperationHandlerFactory {

    @Override
    OperationToSQLConvertHandler createHandler();

    @Component.Builder
    interface Builder {
        @BindsInstance
        Builder setManager(IGraphQLDocumentManager manager);

        @BindsInstance
        Builder setMapper(IGraphQLFieldMapManager mapper);

        OperationToSQLConvertHandlerFactory build();
    }
}
