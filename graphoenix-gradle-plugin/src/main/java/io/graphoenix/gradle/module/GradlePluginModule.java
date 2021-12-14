package io.graphoenix.gradle.module;

import dagger.Module;
import dagger.Provides;
import io.graphoenix.common.module.DocumentManagerModule;
import io.graphoenix.gradle.task.GraphQLSourceGenerator;
import io.graphoenix.graphql.builder.schema.DocumentBuilder;
import io.graphoenix.java.generator.builder.JavaFileBuilder;
import io.graphoenix.java.generator.builder.TypeSpecBuilder;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.config.JavaGeneratorConfig;

import javax.inject.Singleton;

@Module(includes = DocumentManagerModule.class)
public class GradlePluginModule {

    @Provides
    @Singleton
    public DocumentBuilder documentBuilder(IGraphQLDocumentManager manager) {
        return new DocumentBuilder(manager);
    }

    @Provides
    @Singleton
    public TypeSpecBuilder typeSpecBuilder(IGraphQLDocumentManager manager, JavaGeneratorConfig configuration) {
        return new TypeSpecBuilder(manager, configuration);
    }

    @Provides
    @Singleton
    public JavaFileBuilder javaFileBuilder(IGraphQLDocumentManager manager, JavaGeneratorConfig configuration) {
        return new JavaFileBuilder(manager, configuration, typeSpecBuilder(manager, configuration));
    }

    @Provides
    @Singleton
    public GraphQLSourceGenerator graphQLSourceGenerator(IGraphQLDocumentManager manager, JavaGeneratorConfig configuration) {
        return new GraphQLSourceGenerator(documentBuilder(manager), javaFileBuilder(manager, configuration), configuration);
    }
}
