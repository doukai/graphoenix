package io.graphoenix.java.generator.builder;

import com.pivovarit.function.ThrowingBiConsumer;
import com.squareup.javapoet.JavaFile;
import io.graphoenix.spi.config.JavaGeneratorConfig;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import one.util.streamex.StreamEx;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

public class JavaFileBuilder {

    private final IGraphQLDocumentManager manager;
    private final JavaGeneratorConfig configuration;
    private final ThrowingBiConsumer<JavaFile, File, IOException> JavaFileWriteTo = JavaFile::writeTo;

    public JavaFileBuilder(IGraphQLDocumentManager manager, JavaGeneratorConfig configuration) {
        this.manager = manager;
        this.configuration = configuration;
    }

    public void writeToPath(File path) {
        this.buildJavaFileList().forEach(javaFile -> JavaFileWriteTo.uncheck().accept(javaFile, path));
    }

    public Stream<JavaFile> buildJavaFileList() {

        TypeSpecBuilder typeSpecBuilder = new TypeSpecBuilder(manager, configuration);

        return StreamEx.of(manager.getDirectives().map(typeSpecBuilder::buildAnnotation).map(typeSpec -> JavaFile.builder(configuration.getDirectivePackageName(), typeSpec).build()))
                .append(manager.getDirectives()
                        .flatMap(directiveDefinitionContext ->
                                directiveDefinitionContext.argumentsDefinition().inputValueDefinition().stream()
                                        .filter(inputValueDefinitionContext -> manager.isInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type())))
                                        .map(inputValueDefinitionContext -> manager.getInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type())))
                                        .filter(Optional::isPresent)
                                        .map(Optional::get)
                        )
                        .map(typeSpecBuilder::buildAnnotation).map(typeSpec -> JavaFile.builder(configuration.getDirectivePackageName(), typeSpec).build()))
                .append(manager.getEnums().map(typeSpecBuilder::buildEnum).map(typeSpec -> JavaFile.builder(configuration.getEnumTypePackageName(), typeSpec).build()))
                .append(manager.getInterfaces().map(typeSpecBuilder::buildInterface).map(typeSpec -> JavaFile.builder(configuration.getInterfaceTypePackageName(), typeSpec).build()))
                .append(manager.getInputObjects().map(typeSpecBuilder::buildClass).map(typeSpec -> JavaFile.builder(configuration.getInputObjectTypePackageName(), typeSpec).build()))
                .append(manager.getObjects()
                        .filter(objectTypeDefinitionContext ->
                                !manager.isQueryOperationType(objectTypeDefinitionContext.name().getText()) &&
                                        !manager.isMutationOperationType(objectTypeDefinitionContext.name().getText()) &&
                                        !manager.isSubscriptionOperationType(objectTypeDefinitionContext.name().getText())
                        )
                        .map(typeSpecBuilder::buildClass).map(typeSpec -> JavaFile.builder(configuration.getObjectTypePackageName(), typeSpec).build()))
                .append(typeSpecBuilder.buildObjectTypeExpressionAnnotations().map(typeSpec -> JavaFile.builder(configuration.getAnnotationPackageName(), typeSpec).build()))
                .append(typeSpecBuilder.buildObjectTypeExpressionsAnnotations().map(typeSpec -> JavaFile.builder(configuration.getAnnotationPackageName(), typeSpec).build()))
                .append(typeSpecBuilder.buildObjectTypeInputAnnotations().map(typeSpec -> JavaFile.builder(configuration.getAnnotationPackageName(), typeSpec).build()))
                .append(typeSpecBuilder.buildObjectTypeInnerInputAnnotations().map(typeSpec -> JavaFile.builder(configuration.getAnnotationPackageName(), typeSpec).build()));
    }
}
