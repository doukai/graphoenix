package io.graphoenix.java.generator.implementer.grpc;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.handler.FetchHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import org.tinylog.Logger;
import reactor.core.publisher.Mono;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@ApplicationScoped
public class GrpcFetchHandlerBuilder {

    private final IGraphQLDocumentManager manager;
    private final GraphQLConfig graphQLConfig;
    private final GrpcNameUtil grpcNameUtil;
    private Map<String, Map<String, Set<String>>> grpcTypeMap;

    @Inject
    public GrpcFetchHandlerBuilder(IGraphQLDocumentManager manager, GrpcNameUtil grpcNameUtil, GraphQLConfig graphQLConfig) {
        this.manager = manager;
        this.grpcNameUtil = grpcNameUtil;
        this.graphQLConfig = graphQLConfig;
    }

    public void writeToFiler(Filer filer) throws IOException {
        this.buildClass().writeTo(filer);
        Logger.info("GrpcFetchHandler build success");
    }

    private JavaFile buildClass() {
        this.grpcTypeMap = manager.getObjects()
                .flatMap(objectTypeDefinitionContext -> objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream())
                .filter(manager::isFetchField)
                .map(fieldDefinitionContext ->
                        new AbstractMap.SimpleEntry<>(
                                manager.getPackageName(manager.getFieldTypeName(fieldDefinitionContext.type())),
                                new AbstractMap.SimpleEntry<>(
                                        manager.getFieldTypeName(fieldDefinitionContext.type()),
                                        manager.getFetchTo(fieldDefinitionContext)
                                )
                        )
                )
                .collect(
                        Collectors.groupingBy(
                                AbstractMap.SimpleEntry<String, AbstractMap.SimpleEntry<String, String>>::getKey,
                                Collectors.mapping(
                                        AbstractMap.SimpleEntry<String, AbstractMap.SimpleEntry<String, String>>::getValue,
                                        Collectors.toSet()
                                )
                        )
                )
                .entrySet().stream()
                .collect(
                        Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> entry.getValue().stream()
                                        .collect(
                                                Collectors.groupingBy(
                                                        AbstractMap.SimpleEntry<String, String>::getKey,
                                                        Collectors.mapping(
                                                                AbstractMap.SimpleEntry<String, String>::getValue,
                                                                Collectors.toSet()
                                                        )
                                                )
                                        )
                        )
                );
        TypeSpec typeSpec = buildGrpcFetchHandler();
        return JavaFile.builder(graphQLConfig.getGrpcHandlerPackageName(), typeSpec).build();
    }

    private TypeSpec buildGrpcFetchHandler() {
        TypeSpec.Builder builder = TypeSpec.classBuilder("GrpcFetchHandler")
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(ClassName.get(FetchHandler.class))
                .addAnnotation(ApplicationScoped.class)
                .addAnnotation(AnnotationSpec.builder(Named.class).addMember("value", "$S", "grpc").build())
                .addField(
                        FieldSpec.builder(
                                ParameterizedTypeName.get(
                                        ClassName.get(ConcurrentHashMap.class),
                                        ClassName.get(String.class),
                                        ParameterizedTypeName.get(
                                                ClassName.get(Function.class),
                                                ClassName.get(String.class),
                                                ParameterizedTypeName.get(
                                                        ClassName.get(Mono.class),
                                                        ClassName.get(String.class)
                                                )
                                        )
                                ),
                                "operationMap",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).initializer("new $T<>()", ClassName.get(ConcurrentHashMap.class)).build()
                )
                .addMethod(buildConstructor())
                .addMethod(buildOperationMethod());

        if (this.grpcTypeMap.size() > 0) {
            builder.addField(
                    FieldSpec.builder(
                            ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get("io.graphoenix.grpc.client", "ChannelManager")),
                            "channelManagerProvider",
                            Modifier.PRIVATE,
                            Modifier.FINAL
                    ).build()
            );
        }
        grpcTypeMap.keySet()
                .forEach(packageName ->
                        builder.addField(
                                FieldSpec.builder(
                                        ClassName.get(packageName.concat(".grpc"), "ReactorGraphQLServiceGrpc", "ReactorGraphQLServiceStub"),
                                        grpcNameUtil.getGraphQLServiceStubParameterName(packageName),
                                        Modifier.PRIVATE,
                                        Modifier.FINAL
                                ).build()
                        )
                );
        return builder.build();
    }

    private MethodSpec buildConstructor() {
        MethodSpec.Builder builder = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Inject.class);

        if (this.grpcTypeMap.size() > 0) {
            builder.addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get("io.graphoenix.grpc.client", "ChannelManager")), "channelManagerProvider")
                    .addStatement("this.channelManagerProvider = channelManagerProvider");
        }
        this.grpcTypeMap.keySet()
                .forEach(packageName ->
                        builder.addStatement("this.$L = $T.newReactorStub(channelManagerProvider.get().getChannel($S))",
                                grpcNameUtil.getGraphQLServiceStubParameterName(packageName),
                                ClassName.get(packageName.concat(".grpc"), "ReactorGraphQLServiceGrpc"),
                                packageName
                        ).addStatement("this.operationMap.put($S, ($T graphql) -> this.$L.operation($T.newBuilder().setRequest(graphql).build()).map(response -> response.getResponse()))",
                                packageName,
                                ClassName.get(String.class),
                                grpcNameUtil.getGraphQLServiceStubParameterName(packageName),
                                ClassName.get(packageName.concat(".grpc"), "GraphQLRequest")
                        )
                );
        return builder.build();
    }

    private MethodSpec buildOperationMethod() {
        return MethodSpec.methodBuilder("operation")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(ParameterizedTypeName.get(ClassName.get(Mono.class), ClassName.get(String.class)))
                .addParameter(ParameterSpec.builder(ClassName.get(String.class), "packageName").build())
                .addParameter(ParameterSpec.builder(ClassName.get(String.class), "graphql").build())
                .addStatement("return operationMap.get(packageName).apply(graphql)")
                .build();
    }
}
