package io.graphoenix.java.generator.builder;

import com.google.common.collect.Streams;
import com.squareup.javapoet.JavaFile;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.vavr.control.Try;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.tinylog.Logger;

import java.io.File;
import java.util.Optional;
import java.util.stream.Stream;

@ApplicationScoped
public class JavaFileBuilder {

    private final IGraphQLDocumentManager manager;
    private final TypeSpecBuilder typeSpecBuilder;
    private final GraphQLConfig graphQLConfig;

    @Inject
    public JavaFileBuilder(IGraphQLDocumentManager manager, GraphQLConfig graphQLConfig, TypeSpecBuilder typeSpecBuilder) {
        this.manager = manager;
        this.graphQLConfig = graphQLConfig;
        this.typeSpecBuilder = typeSpecBuilder;
    }

    public void writeToPath(File path) {
        this.buildJavaFileList().forEach(javaFile -> Try.run(() -> javaFile.writeTo(path)));
        Logger.info("all graphql entity generated");
    }

    public void writeToPath(File path, GraphQLConfig graphQLConfig) {
        this.buildJavaFileList(graphQLConfig, typeSpecBuilder.setConfiguration(graphQLConfig)).forEach(javaFile -> Try.run(() -> javaFile.writeTo(path)));
        Logger.info("all graphql entity generated");
    }

    public Stream<JavaFile> buildJavaFileList() {
        return buildJavaFileList(graphQLConfig, typeSpecBuilder);
    }

    public Stream<JavaFile> buildJavaFileList(GraphQLConfig configuration, TypeSpecBuilder typeSpecBuilder) {
        return Streams.concat(
                manager.getDirectives().map(typeSpecBuilder::buildAnnotation).map(typeSpec -> JavaFile.builder(configuration.getDirectivePackageName(), typeSpec).build()),
                manager.getDirectives()
                        .filter(directiveDefinitionContext -> directiveDefinitionContext.argumentsDefinition() != null)
                        .flatMap(directiveDefinitionContext ->
                                directiveDefinitionContext.argumentsDefinition().inputValueDefinition().stream()
                                        .filter(inputValueDefinitionContext -> manager.isInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type())))
                                        .map(inputValueDefinitionContext -> manager.getInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type())))
                                        .filter(Optional::isPresent)
                                        .map(Optional::get)
                        )
                        .map(typeSpecBuilder::buildAnnotation).map(typeSpec -> JavaFile.builder(configuration.getDirectivePackageName(), typeSpec).build()),
                manager.getEnums().map(typeSpecBuilder::buildEnum).map(typeSpec -> JavaFile.builder(configuration.getEnumTypePackageName(), typeSpec).build()),
                manager.getInterfaces().map(typeSpecBuilder::buildInterface).map(typeSpec -> JavaFile.builder(configuration.getInterfaceTypePackageName(), typeSpec).build()),
                manager.getInputObjects().map(typeSpecBuilder::buildClass).map(typeSpec -> JavaFile.builder(configuration.getInputObjectTypePackageName(), typeSpec).build()),
                manager.getObjects()
                        .filter(objectTypeDefinitionContext ->
                                !manager.isQueryOperationType(objectTypeDefinitionContext.name().getText()) &&
                                        !manager.isMutationOperationType(objectTypeDefinitionContext.name().getText()) &&
                                        !manager.isSubscriptionOperationType(objectTypeDefinitionContext.name().getText())
                        )
                        .map(typeSpecBuilder::buildClass).map(typeSpec -> JavaFile.builder(configuration.getObjectTypePackageName(), typeSpec).build()),
                typeSpecBuilder.buildScalarTypeExpressionAnnotations().map(typeSpec -> JavaFile.builder(configuration.getAnnotationPackageName(), typeSpec).build()),
                typeSpecBuilder.buildEnumTypeExpressionAnnotations().map(typeSpec -> JavaFile.builder(configuration.getAnnotationPackageName(), typeSpec).build()),
                typeSpecBuilder.buildObjectTypeExpressionAnnotations().map(typeSpec -> JavaFile.builder(configuration.getAnnotationPackageName(), typeSpec).build()),
                typeSpecBuilder.buildObjectTypeInputAnnotations().map(typeSpec -> JavaFile.builder(configuration.getAnnotationPackageName(), typeSpec).build()),
                typeSpecBuilder.buildObjectTypeOrderByAnnotations().map(typeSpec -> JavaFile.builder(configuration.getAnnotationPackageName(), typeSpec).build())
        );
    }
}
