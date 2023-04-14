package io.graphoenix.core.handler;

import com.google.common.reflect.ClassPath;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.spi.annotation.Package;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.tinylog.Logger;

import java.io.IOException;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

@ApplicationScoped
public class PackageManager {

    private final GraphQLConfig graphQLConfig;
    private final IGraphQLDocumentManager manager;

    @Inject
    public PackageManager(GraphQLConfig graphQLConfig, IGraphQLDocumentManager manager) {
        this.graphQLConfig = graphQLConfig;
        this.manager = manager;
    }

    public String getDefaultPackageName() {
        try {
            ClassPath classPath = ClassPath.from(Thread.currentThread().getContextClassLoader());
            return classPath.getTopLevelClasses()
                    .stream()
                    .filter(classInfo -> classInfo.getSimpleName().equals("package-info"))
                    .filter(classInfo -> classInfo.load().getPackage().isAnnotationPresent(Package.class))
                    .findFirst()
                    .map(ClassPath.ClassInfo::getPackageName)
                    .orElseThrow(() -> new RuntimeException("package name not exist"));
        } catch (IOException e) {
            Logger.error(e);
        }
        throw new RuntimeException("package name not exist");
    }

    public boolean isOwnPackage(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        return manager.getPackageName(objectTypeDefinitionContext).map(this::isOwnPackage).orElse(true);
    }

    public boolean isOwnPackage(GraphqlParser.EnumTypeDefinitionContext enumTypeDefinitionContext) {
        return manager.getPackageName(enumTypeDefinitionContext).map(this::isOwnPackage).orElse(true);
    }

    public boolean isOwnPackage(GraphqlParser.InterfaceTypeDefinitionContext interfaceTypeDefinitionContext) {
        return manager.getPackageName(interfaceTypeDefinitionContext).map(this::isOwnPackage).orElse(true);
    }

    public boolean isOwnPackage(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext) {
        return manager.getPackageName(inputObjectTypeDefinitionContext).map(this::isOwnPackage).orElse(true);
    }

    public boolean isOwnPackage(GraphqlParser.OperationDefinitionContext operationDefinitionContext) {
        return manager.getPackageName(operationDefinitionContext).map(this::isOwnPackage).orElse(true);
    }

    public boolean isOwnPackage(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return manager.getPackageName(fieldDefinitionContext).map(this::isOwnPackage).orElse(true);
    }

    public boolean isOwnPackage(GraphqlParser.TypeContext typeContext) {
        return manager.getPackageName(typeContext).map(this::isOwnPackage).orElse(true);
    }

    public boolean isNotOwnPackage(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        return !isOwnPackage(objectTypeDefinitionContext);
    }

    public boolean isNotOwnPackage(String packageName) {
        return !isOwnPackage(packageName);
    }

    public boolean isNotOwnPackage(GraphqlParser.EnumTypeDefinitionContext enumTypeDefinitionContext) {
        return !isOwnPackage(enumTypeDefinitionContext);
    }

    public boolean isNotOwnPackage(GraphqlParser.InterfaceTypeDefinitionContext interfaceTypeDefinitionContext) {
        return !isOwnPackage(interfaceTypeDefinitionContext);
    }

    public boolean isNotOwnPackage(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext) {
        return !isOwnPackage(inputObjectTypeDefinitionContext);
    }

    public boolean isNotOwnPackage(GraphqlParser.TypeContext typeContext) {
        return !isOwnPackage(typeContext);
    }

    public boolean isOwnPackage(String packageName) {
        return Optional.ofNullable(graphQLConfig.getPackageName()).orElseGet(this::getDefaultPackageName).equals(packageName);
    }

    public boolean isLocalPackage(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        return manager.getPackageName(objectTypeDefinitionContext).map(this::isLocalPackage).orElse(true);
    }

    public boolean isLocalPackage(GraphqlParser.EnumTypeDefinitionContext enumTypeDefinitionContext) {
        return manager.getPackageName(enumTypeDefinitionContext).map(this::isLocalPackage).orElse(true);
    }

    public boolean isLocalPackage(GraphqlParser.InterfaceTypeDefinitionContext interfaceTypeDefinitionContext) {
        return manager.getPackageName(interfaceTypeDefinitionContext).map(this::isLocalPackage).orElse(true);
    }

    public boolean isLocalPackage(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext) {
        return manager.getPackageName(inputObjectTypeDefinitionContext).map(this::isLocalPackage).orElse(true);
    }

    public boolean isLocalPackage(GraphqlParser.OperationDefinitionContext operationDefinitionContext) {
        return manager.getPackageName(operationDefinitionContext).map(this::isLocalPackage).orElse(true);
    }

    public boolean isLocalPackage(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return manager.getPackageName(fieldDefinitionContext).map(this::isLocalPackage).orElse(true);
    }

    public boolean isLocalPackage(String typeName, String fieldName) {
        return manager.getField(typeName, fieldName).map(this::isLocalPackage).orElse(true);
    }

    public boolean isNotLocalPackage(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        return !isLocalPackage(objectTypeDefinitionContext);
    }

    public boolean isNotLocalPackage(GraphqlParser.EnumTypeDefinitionContext enumTypeDefinitionContext) {
        return !isLocalPackage(enumTypeDefinitionContext);
    }

    public boolean isNotLocalPackage(GraphqlParser.InterfaceTypeDefinitionContext interfaceTypeDefinitionContext) {
        return !isLocalPackage(interfaceTypeDefinitionContext);
    }

    public boolean isNotLocalPackage(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext) {
        return !isLocalPackage(inputObjectTypeDefinitionContext);
    }

    public boolean isNotLocalPackage(GraphqlParser.OperationDefinitionContext operationDefinitionContext) {
        return !isLocalPackage(operationDefinitionContext);
    }

    public boolean isNotLocalPackage(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return !isLocalPackage(fieldDefinitionContext);
    }

    public boolean isNotLocalPackage(String typeName, String fieldName) {
        return !isLocalPackage(typeName, fieldName);
    }

    public boolean isLocalPackage(String packageName) {
        return getLocalPackages().anyMatch(localPackageName -> localPackageName.equals(packageName));
    }

    public Stream<String> getLocalPackages() {
        return Stream.concat(
                Stream.of(Optional.ofNullable(graphQLConfig.getPackageName()).orElseGet(this::getDefaultPackageName)),
                Stream.ofNullable(graphQLConfig.getLocalPackageNames()).flatMap(Collection::stream)
        );
    }
}
