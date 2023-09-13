package io.graphoenix.core.handler;

import com.google.common.reflect.ClassPath;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.spi.annotation.Package;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.tinylog.Logger;

import java.io.IOException;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

import static io.graphoenix.core.error.GraphQLErrorType.*;

@ApplicationScoped
public class PackageManager {

    private final GraphQLConfig graphQLConfig;
    private final IGraphQLDocumentManager manager;

    @Inject
    public PackageManager(GraphQLConfig graphQLConfig, IGraphQLDocumentManager manager) {
        this.graphQLConfig = graphQLConfig;
        this.manager = manager;
    }

    public Optional<String> getDefaultPackageName() {
        try {
            ClassPath classPath = ClassPath.from(Thread.currentThread().getContextClassLoader());
            return classPath.getTopLevelClasses()
                    .stream()
                    .filter(classInfo -> classInfo.getSimpleName().equals("package-info"))
                    .filter(classInfo -> classInfo.load().getPackage().isAnnotationPresent(Package.class))
                    .findFirst()
                    .map(ClassPath.ClassInfo::getPackageName);
        } catch (IOException e) {
            Logger.error(e);
        }
        return Optional.empty();
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
        return graphQLConfig.getPackageName().equals(packageName);
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
                Stream.ofNullable(graphQLConfig.getPackageName()),
                Stream.ofNullable(graphQLConfig.getLocalPackageNames()).flatMap(Collection::stream)
        );
    }

    public String getClassName(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        return manager.getClassName(objectTypeDefinitionContext).orElseGet(() -> graphQLConfig.getObjectTypePackageName() + "." + objectTypeDefinitionContext.name().getText());
    }

    public String getClassName(GraphqlParser.EnumTypeDefinitionContext enumTypeDefinitionContext) {
        return manager.getClassName(enumTypeDefinitionContext).orElseGet(() -> graphQLConfig.getEnumTypePackageName() + "." + enumTypeDefinitionContext.name().getText());
    }

    public String getClassName(GraphqlParser.InterfaceTypeDefinitionContext interfaceTypeDefinitionContext) {
        return manager.getClassName(interfaceTypeDefinitionContext).orElseGet(() -> graphQLConfig.getInterfaceTypePackageName() + "." + interfaceTypeDefinitionContext.name().getText());
    }

    public String getClassName(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext) {
        return manager.getClassName(inputObjectTypeDefinitionContext).orElseGet(() -> graphQLConfig.getInputObjectTypePackageName() + "." + inputObjectTypeDefinitionContext.name().getText());
    }

    public String getClassName(GraphqlParser.TypeContext typeContext) {
        String fieldTypeName = manager.getFieldTypeName(typeContext);
        if (manager.isObject(fieldTypeName)) {
            return manager.getObject(fieldTypeName).map(this::getClassName).orElseThrow(() -> new GraphQLErrors(TYPE_NOT_EXIST.bind(fieldTypeName)));
        } else if (manager.isInterface(fieldTypeName)) {
            return manager.getInterface(fieldTypeName).map(this::getClassName).orElseThrow(() -> new GraphQLErrors(TYPE_NOT_EXIST.bind(fieldTypeName)));
        } else if (manager.isEnum(fieldTypeName)) {
            return manager.getEnum(fieldTypeName).map(this::getClassName).orElseThrow(() -> new GraphQLErrors(TYPE_NOT_EXIST.bind(fieldTypeName)));
        } else if (manager.isInputObject(fieldTypeName)) {
            return manager.getInputObject(fieldTypeName).map(this::getClassName).orElseThrow(() -> new GraphQLErrors(TYPE_NOT_EXIST.bind(fieldTypeName)));
        }
        throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE.bind(fieldTypeName));
    }

    public String getAnnotationName(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext) {
        return manager.getAnnotationName(inputObjectTypeDefinitionContext).orElseGet(() -> graphQLConfig.getAnnotationPackageName() + "." + inputObjectTypeDefinitionContext.name().getText());
    }

    public String getAnnotationName(GraphqlParser.TypeContext typeContext) {
        String fieldTypeName = manager.getFieldTypeName(typeContext);
        if (manager.isInputObject(fieldTypeName)) {
            return manager.getInputObject(fieldTypeName).map(this::getAnnotationName).orElseThrow(() -> new GraphQLErrors(TYPE_NOT_EXIST.bind(fieldTypeName)));
        }
        throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE.bind(fieldTypeName));
    }

    public String getGrpcClassName(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        return manager.getGrpcClassName(objectTypeDefinitionContext).orElseGet(() -> graphQLConfig.getGrpcObjectTypePackageName() + "." + objectTypeDefinitionContext.name().getText());
    }

    public String getGrpcClassName(GraphqlParser.EnumTypeDefinitionContext enumTypeDefinitionContext) {
        return manager.getGrpcClassName(enumTypeDefinitionContext).orElseGet(() -> graphQLConfig.getGrpcEnumTypePackageName() + "." + enumTypeDefinitionContext.name().getText());
    }

    public String getGrpcClassName(GraphqlParser.InterfaceTypeDefinitionContext interfaceTypeDefinitionContext) {
        return manager.getGrpcClassName(interfaceTypeDefinitionContext).orElseGet(() -> graphQLConfig.getGrpcInterfaceTypePackageName() + "." + interfaceTypeDefinitionContext.name().getText());
    }

    public String getGrpcClassName(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext) {
        return manager.getGrpcClassName(inputObjectTypeDefinitionContext).orElseGet(() -> graphQLConfig.getGrpcInputObjectTypePackageName() + "." + inputObjectTypeDefinitionContext.name().getText());
    }

    public String getGrpcClassName(GraphqlParser.TypeContext typeContext) {
        String fieldTypeName = manager.getFieldTypeName(typeContext);
        if (manager.isObject(fieldTypeName)) {
            return manager.getObject(fieldTypeName).map(this::getGrpcClassName).orElseThrow(() -> new GraphQLErrors(TYPE_NOT_EXIST.bind(fieldTypeName)));
        } else if (manager.isInterface(fieldTypeName)) {
            return manager.getInterface(fieldTypeName).map(this::getGrpcClassName).orElseThrow(() -> new GraphQLErrors(TYPE_NOT_EXIST.bind(fieldTypeName)));
        } else if (manager.isEnum(fieldTypeName)) {
            return manager.getEnum(fieldTypeName).map(this::getGrpcClassName).orElseThrow(() -> new GraphQLErrors(TYPE_NOT_EXIST.bind(fieldTypeName)));
        } else if (manager.isInputObject(fieldTypeName)) {
            return manager.getInputObject(fieldTypeName).map(this::getGrpcClassName).orElseThrow(() -> new GraphQLErrors(TYPE_NOT_EXIST.bind(fieldTypeName)));
        }
        throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE.bind(fieldTypeName));
    }
}
