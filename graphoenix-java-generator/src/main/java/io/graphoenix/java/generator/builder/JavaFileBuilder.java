package io.graphoenix.java.generator.builder;

import com.google.common.collect.Streams;
import com.squareup.javapoet.JavaFile;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.handler.PackageManager;
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
    private final PackageManager packageManager;
    private final TypeSpecBuilder typeSpecBuilder;
    private final GraphQLConfig graphQLConfig;

    @Inject
    public JavaFileBuilder(IGraphQLDocumentManager manager, PackageManager packageManager, GraphQLConfig graphQLConfig, TypeSpecBuilder typeSpecBuilder) {
        this.manager = manager;
        this.packageManager = packageManager;
        this.graphQLConfig = graphQLConfig;
        this.typeSpecBuilder = typeSpecBuilder;
    }

    public void writeToPath(File path) {
        this.buildJavaFileList().forEach(javaFile -> Try.run(() -> javaFile.writeTo(path)));
        Logger.info("all graphql entity generated");
    }

    public Stream<JavaFile> buildJavaFileList() {
        return Streams.concat(
                manager.getDirectives().map(typeSpecBuilder::buildAnnotation).map(typeSpec -> JavaFile.builder(graphQLConfig.getAnnotationPackageName(), typeSpec).build()),
                manager.getDirectives()
                        .filter(directiveDefinitionContext -> directiveDefinitionContext.argumentsDefinition() != null)
                        .flatMap(directiveDefinitionContext ->
                                directiveDefinitionContext.argumentsDefinition().inputValueDefinition().stream()
                                        .filter(inputValueDefinitionContext -> manager.isInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type())))
                                        .map(inputValueDefinitionContext -> manager.getInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type())))
                                        .flatMap(Optional::stream)
                                        .filter(packageManager::isOwnPackage)
                        )
                        .map(typeSpecBuilder::buildAnnotation)
                        .map(typeSpec -> JavaFile.builder(graphQLConfig.getAnnotationPackageName(), typeSpec).build()),
                manager.getEnums()
                        .filter(packageManager::isOwnPackage)
                        .filter(enumTypeDefinitionContext -> manager.getClassName(enumTypeDefinitionContext).isEmpty())
                        .map(typeSpecBuilder::buildEnum)
                        .map(typeSpec -> JavaFile.builder(graphQLConfig.getEnumTypePackageName(), typeSpec).build()),
                manager.getInterfaces()
                        .filter(packageManager::isOwnPackage)
                        .filter(interfaceTypeDefinitionContext -> manager.getClassName(interfaceTypeDefinitionContext).isEmpty())
                        .map(typeSpecBuilder::buildInterface)
                        .map(typeSpec -> JavaFile.builder(graphQLConfig.getInterfaceTypePackageName(), typeSpec).build()),
                manager.getInputObjects()
                        .filter(packageManager::isOwnPackage)
                        .filter(inputObjectTypeDefinitionContext -> manager.getClassName(inputObjectTypeDefinitionContext).isEmpty())
                        .map(typeSpecBuilder::buildClass)
                        .map(typeSpec -> JavaFile.builder(graphQLConfig.getInputObjectTypePackageName(), typeSpec).build()),
                manager.getObjects()
                        .filter(packageManager::isOwnPackage)
                        .filter(objectTypeDefinitionContext -> manager.getClassName(objectTypeDefinitionContext).isEmpty())
                        .map(typeSpecBuilder::buildClass)
                        .map(typeSpec -> JavaFile.builder(graphQLConfig.getObjectTypePackageName(), typeSpec).build()),
                typeSpecBuilder.buildScalarTypeExpressionAnnotations().map(typeSpec -> JavaFile.builder(graphQLConfig.getAnnotationPackageName(), typeSpec).build()),
                typeSpecBuilder.buildEnumTypeExpressionAnnotations().map(typeSpec -> JavaFile.builder(graphQLConfig.getAnnotationPackageName(), typeSpec).build()),
                typeSpecBuilder.buildObjectTypeExpressionAnnotations().map(typeSpec -> JavaFile.builder(graphQLConfig.getAnnotationPackageName(), typeSpec).build()),
                typeSpecBuilder.buildObjectTypeInputAnnotations().map(typeSpec -> JavaFile.builder(graphQLConfig.getAnnotationPackageName(), typeSpec).build()),
                typeSpecBuilder.buildObjectTypeOrderByAnnotations().map(typeSpec -> JavaFile.builder(graphQLConfig.getAnnotationPackageName(), typeSpec).build())
        );
    }
}
