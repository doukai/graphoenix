package io.graphoenix.java.generator.implementer.grpc;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.core.handler.PackageManager;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.dto.type.OperationType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.tinylog.Logger;
import reactor.core.publisher.Mono;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.graphoenix.core.error.GraphQLErrorType.MUTATION_TYPE_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.QUERY_TYPE_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.UNSUPPORTED_OPERATION_TYPE;
import static io.graphoenix.spi.constant.Hammurabi.MUTATION_TYPE_NAME;
import static io.graphoenix.spi.constant.Hammurabi.QUERY_TYPE_NAME;
import static io.graphoenix.spi.dto.type.OperationType.MUTATION;
import static io.graphoenix.spi.dto.type.OperationType.QUERY;

@ApplicationScoped
public class GrpcServiceImplementer {

    private final IGraphQLDocumentManager manager;
    private final PackageManager packageManager;
    private final GrpcNameUtil grpcNameUtil;
    private final GraphQLConfig graphQLConfig;

    @Inject
    public GrpcServiceImplementer(IGraphQLDocumentManager manager, PackageManager packageManager, GrpcNameUtil grpcNameUtil, GraphQLConfig graphQLConfig) {
        this.manager = manager;
        this.packageManager = packageManager;
        this.grpcNameUtil = grpcNameUtil;
        this.graphQLConfig = graphQLConfig;
    }

    public void writeToFiler(Filer filer) throws IOException {
        manager.getQueryOperationTypeName()
                .flatMap(manager::getObject)
                .orElseThrow(() -> new GraphQLErrors(QUERY_TYPE_NOT_EXIST))
                .fieldsDefinition().fieldDefinition().stream()
                .filter(packageManager::isLocalPackage)
                .map(fieldDefinitionContext -> new AbstractMap.SimpleEntry<>(manager.getPackageName(fieldDefinitionContext).orElseGet(graphQLConfig::getPackageName), fieldDefinitionContext))
                .collect(
                        Collectors.groupingBy(
                                Map.Entry<String, GraphqlParser.FieldDefinitionContext>::getKey,
                                Collectors.mapping(
                                        Map.Entry<String, GraphqlParser.FieldDefinitionContext>::getValue,
                                        Collectors.toList()
                                )
                        )
                )
                .forEach((packageName, fieldDefinitionContextList) -> {
                            try {
                                this.buildTypeServiceImplClass(packageName, QUERY, fieldDefinitionContextList).writeTo(filer);
                                Logger.info("{}.Grpc" + manager.getQueryOperationTypeName().orElse(QUERY_TYPE_NAME) + "ServiceImpl build success", packageName);
                            } catch (IOException e) {
                                Logger.error(e);
                            }
                        }
                );

        manager.getMutationOperationTypeName()
                .flatMap(manager::getObject)
                .orElseThrow(() -> new GraphQLErrors(MUTATION_TYPE_NOT_EXIST))
                .fieldsDefinition().fieldDefinition().stream()
                .filter(packageManager::isLocalPackage)
                .map(fieldDefinitionContext -> new AbstractMap.SimpleEntry<>(manager.getPackageName(fieldDefinitionContext).orElseGet(graphQLConfig::getPackageName), fieldDefinitionContext))
                .collect(
                        Collectors.groupingBy(
                                Map.Entry<String, GraphqlParser.FieldDefinitionContext>::getKey,
                                Collectors.mapping(
                                        Map.Entry<String, GraphqlParser.FieldDefinitionContext>::getValue,
                                        Collectors.toList()
                                )
                        )
                )
                .forEach((packageName, fieldDefinitionContextList) -> {
                            try {
                                this.buildTypeServiceImplClass(packageName, MUTATION, fieldDefinitionContextList).writeTo(filer);
                                Logger.info("{}.Grpc" + manager.getMutationOperationTypeName().orElse(MUTATION_TYPE_NAME) + "ServiceImpl build success", packageName);
                            } catch (IOException e) {
                                Logger.error(e);
                            }
                        }
                );

        manager.getQueryOperationTypeName()
                .flatMap(manager::getObject)
                .stream()
                .flatMap(objectTypeDefinitionContext -> objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream())
                .filter(packageManager::isLocalPackage)
                .map(fieldDefinitionContext -> manager.getPackageName(fieldDefinitionContext).orElseGet(graphQLConfig::getPackageName))
                .distinct()
                .forEach(packageName -> {
                            try {
                                this.buildGraphQLServiceImplClass(packageName).writeTo(filer);
                                Logger.info("{}.GrpcGraphQLServiceImpl build success", packageName);
                            } catch (IOException e) {
                                Logger.error(e);
                            }
                        }
                );
    }

    private JavaFile buildTypeServiceImplClass(String packageName, OperationType operationType, List<GraphqlParser.FieldDefinitionContext> fieldDefinitionContextList) {
        TypeSpec typeSpec = buildOperationTypeServiceImpl(packageName, operationType, fieldDefinitionContextList);
        return JavaFile.builder(packageName + ".grpc", typeSpec).build();
    }

    private JavaFile buildGraphQLServiceImplClass(String packageName) {
        TypeSpec typeSpec = buildGraphQLServiceImpl(packageName);
        return JavaFile.builder(packageName + ".grpc", typeSpec).build();
    }

    private TypeSpec buildOperationTypeServiceImpl(String packageName, OperationType operationType, List<GraphqlParser.FieldDefinitionContext> fieldDefinitionContextList) {
        String grpcPackageName = packageName + ".grpc";
        String className;
        String reactorClassName;
        String superClassName;
        String reactorSuperClassName;
        String serviceName;
        switch (operationType) {
            case QUERY:
                className = "Grpc" + manager.getQueryOperationTypeName().orElse(QUERY_TYPE_NAME) + "ServiceImpl";
                reactorClassName = "ReactorGrpc" + manager.getQueryOperationTypeName().orElse(QUERY_TYPE_NAME) + "ServiceImpl";
                superClassName = manager.getQueryOperationTypeName().orElse(QUERY_TYPE_NAME) + "ServiceGrpc";
                reactorSuperClassName = "Reactor" + manager.getQueryOperationTypeName().orElse(QUERY_TYPE_NAME) + "ServiceGrpc";
                serviceName = manager.getQueryOperationTypeName().orElse(QUERY_TYPE_NAME) + "ServiceImplBase";
                break;
            case MUTATION:
                className = "Grpc" + manager.getMutationOperationTypeName().orElse(MUTATION_TYPE_NAME) + "ServiceImpl";
                reactorClassName = "ReactorGrpc" + manager.getMutationOperationTypeName().orElse(MUTATION_TYPE_NAME) + "ServiceImpl";
                superClassName = manager.getMutationOperationTypeName().orElse(MUTATION_TYPE_NAME) + "ServiceGrpc";
                reactorSuperClassName = "Reactor" + manager.getMutationOperationTypeName().orElse(MUTATION_TYPE_NAME) + "ServiceGrpc";
                serviceName = manager.getMutationOperationTypeName().orElse(MUTATION_TYPE_NAME) + "ServiceImplBase";
                break;
            default:
                throw new GraphQLErrors(UNSUPPORTED_OPERATION_TYPE);
        }

        return TypeSpec.classBuilder(className)
                .superclass(ClassName.get(grpcPackageName, superClassName, serviceName))
                .addModifiers(Modifier.PUBLIC)
                .addField(
                        FieldSpec.builder(
                                ClassName.get(grpcPackageName, reactorSuperClassName, serviceName),
                                "reactorService",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        )
                                .initializer("new $T()", ClassName.get(grpcPackageName, reactorClassName))
                                .build()
                )
                .addMethods(buildTypeMethods(packageName, operationType, fieldDefinitionContextList))
                .build();
    }

    private List<MethodSpec> buildTypeMethods(String packageName, OperationType operationType, List<GraphqlParser.FieldDefinitionContext> fieldDefinitionContextList) {
        return fieldDefinitionContextList.stream()
                .map(fieldDefinitionContext -> buildTypeMethod(packageName, operationType, fieldDefinitionContext))
                .collect(Collectors.toList());
    }

    private MethodSpec buildTypeMethod(String packageName, OperationType operationType, GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        String grpcPackageName = packageName + ".grpc";
        String requestParameterName = "request";
        String responseObserverParameterName = "responseObserver";
        String grpcHandlerMethodName = grpcNameUtil.getGrpcFieldName(fieldDefinitionContext);
        String grpcRequestClassName = grpcNameUtil.getGrpcRequestClassName(fieldDefinitionContext, operationType);
        String grpcResponseClassName = grpcNameUtil.getGrpcResponseClassName(fieldDefinitionContext, operationType);

        ClassName requestClassName = ClassName.get(grpcPackageName, grpcRequestClassName);
        ParameterizedTypeName responseClassName = ParameterizedTypeName.get(ClassName.get("io.grpc.stub", "StreamObserver"), ClassName.get(grpcPackageName, grpcResponseClassName));
        return MethodSpec.methodBuilder(grpcHandlerMethodName)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(requestClassName, requestParameterName)
                .addParameter(responseClassName, responseObserverParameterName)
                .addStatement("reactorService.$L($T.just($L)).subscribe($L::onNext, $L::onError, $L::onCompleted)", grpcHandlerMethodName, ClassName.get(Mono.class), requestParameterName, responseObserverParameterName, responseObserverParameterName, responseObserverParameterName)
                .build();
    }

    private TypeSpec buildGraphQLServiceImpl(String packageName) {
        String grpcPackageName = packageName + ".grpc";
        return TypeSpec.classBuilder("GrpcGraphQLServiceImpl")
                .superclass(ClassName.get(grpcPackageName, "GraphQLServiceGrpc", "GraphQLServiceImplBase"))
                .addModifiers(Modifier.PUBLIC)
                .addField(
                        FieldSpec.builder(
                                ClassName.get(grpcPackageName, "ReactorGraphQLServiceGrpc", "GraphQLServiceImplBase"),
                                "reactorService",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        )
                                .initializer("new $T()", ClassName.get(grpcPackageName, "ReactorGrpcGraphQLServiceImpl"))
                                .build()
                )
                .addMethod(buildOperationMethod(packageName))
                .build();
    }

    private MethodSpec buildOperationMethod(String packageName) {
        String grpcPackageName = packageName + ".grpc";
        String requestParameterName = "request";
        String responseObserverParameterName = "responseObserver";
        ClassName requestClassName = ClassName.get(grpcPackageName, "GraphQLRequest");
        ParameterizedTypeName responseClassName = ParameterizedTypeName.get(ClassName.get("io.grpc.stub", "StreamObserver"), ClassName.get(grpcPackageName, "GraphQLResponse"));

        return MethodSpec.methodBuilder("request")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(requestClassName, requestParameterName)
                .addParameter(responseClassName, responseObserverParameterName)
                .addStatement("reactorService.$L($T.just($L)).subscribe($L::onNext, $L::onError, $L::onCompleted)", "request", ClassName.get(Mono.class), requestParameterName, responseObserverParameterName, responseObserverParameterName, responseObserverParameterName)
                .build();
    }
}
