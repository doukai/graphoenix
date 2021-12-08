package io.graphoenix.graphql.generator.module;

import dagger.Module;
import dagger.Provides;
import io.graphoenix.common.module.DocumentManagerModule;
import io.graphoenix.common.utils.YamlConfigUtil;
import io.graphoenix.graphql.generator.translator.ElementManager;
import io.graphoenix.graphql.generator.translator.JavaElementToOperation;
import io.graphoenix.graphql.generator.translator.MethodToMutationOperation;
import io.graphoenix.graphql.generator.translator.MethodToQueryOperation;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.config.JavaGeneratorConfig;

import javax.inject.Singleton;
import java.io.FileNotFoundException;

@Module(includes = DocumentManagerModule.class)
public class GraphQLGeneratorModule {

    @Provides
    @Singleton
    JavaGeneratorConfig javaGeneratorConfig() {
        try {
            return YamlConfigUtil.YAML_CONFIG_UTIL.loadAs(JavaGeneratorConfig.class);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Provides
    @Singleton
    ElementManager elementManager(IGraphQLDocumentManager manager) {
        return new ElementManager(manager);
    }

    @Provides
    @Singleton
    MethodToQueryOperation methodToQueryOperation(IGraphQLDocumentManager manager) {
        return new MethodToQueryOperation(manager, javaGeneratorConfig(), elementManager(manager));
    }

    @Provides
    @Singleton
    MethodToMutationOperation methodToMutationOperation(IGraphQLDocumentManager manager) {
        return new MethodToMutationOperation(manager, elementManager(manager));
    }

    @Provides
    @Singleton
    JavaElementToOperation javaElementToOperation(IGraphQLDocumentManager manager) {
        return new JavaElementToOperation(methodToQueryOperation(manager), methodToMutationOperation(manager));
    }
}
