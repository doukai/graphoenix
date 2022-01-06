package io.graphoenix.java.generator.builder;

import com.google.common.collect.Streams;
import com.squareup.javapoet.JavaFile;
import io.graphoenix.java.generator.config.JavaGeneratorConfig;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.vavr.control.Try;

import javax.inject.Inject;
import java.io.File;
import java.util.Optional;
import java.util.stream.Stream;

public class JavaFileBuilder {

    private final IGraphQLDocumentManager manager;
    private final TypeSpecBuilder typeSpecBuilder;
    private final JavaGeneratorConfig configuration;

    @Inject
    public JavaFileBuilder(IGraphQLDocumentManager manager, JavaGeneratorConfig configuration, TypeSpecBuilder typeSpecBuilder) {
        this.manager = manager;
        this.configuration = configuration;
        this.typeSpecBuilder = typeSpecBuilder;
    }

    public void writeToPath(File path) {
        this.buildJavaFileList().forEach(javaFile -> Try.run(() -> javaFile.writeTo(path)));
    }

    public void writeToPath(File path, JavaGeneratorConfig configuration) {
        this.buildJavaFileList(configuration, typeSpecBuilder.setConfiguration(configuration)).forEach(javaFile -> Try.run(() -> javaFile.writeTo(path)));
    }

    public Stream<JavaFile> buildJavaFileList() {
        return buildJavaFileList(configuration, typeSpecBuilder);
    }

    public Stream<JavaFile> buildJavaFileList(JavaGeneratorConfig configuration, TypeSpecBuilder typeSpecBuilder) {

        return Streams.concat(
                manager.getDirectives().map(typeSpecBuilder::buildAnnotation).map(typeSpec -> JavaFile.builder(configuration.getDirectivePackageName(), typeSpec).build()),
                manager.getDirectives()
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
                typeSpecBuilder.buildObjectTypeExpressionAnnotations().map(typeSpec -> JavaFile.builder(configuration.getAnnotationPackageName(), typeSpec).build()),
                typeSpecBuilder.buildObjectTypeExpressionsAnnotations().map(typeSpec -> JavaFile.builder(configuration.getAnnotationPackageName(), typeSpec).build()),
                typeSpecBuilder.buildObjectTypeInputAnnotations().map(typeSpec -> JavaFile.builder(configuration.getAnnotationPackageName(), typeSpec).build()),
                typeSpecBuilder.buildObjectTypeInnerInputAnnotations().map(typeSpec -> JavaFile.builder(configuration.getAnnotationPackageName(), typeSpec).build())
        );
    }
}
