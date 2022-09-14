package io.graphoenix.java.generator.implementer.grpc;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.handler.QueryDataLoader;
import io.graphoenix.java.generator.implementer.TypeManager;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.json.JsonValue;
import jakarta.json.spi.JsonProvider;
import org.tinylog.Logger;
import reactor.core.publisher.Mono;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class GrpcQueryDataLoaderBuilder {

    private final IGraphQLDocumentManager manager;
    private final TypeManager typeManager;
    private GraphQLConfig graphQLConfig;
    private final GrpcNameUtil grpcNameUtil;
    private final Map<String, Map<String, Set<String>>> typeMap;

    @Inject
    public GrpcQueryDataLoaderBuilder(IGraphQLDocumentManager manager, TypeManager typeManager, GrpcNameUtil grpcNameUtil) {
        this.manager = manager;
        this.typeManager = typeManager;
        this.grpcNameUtil = grpcNameUtil;
        this.typeMap = manager.getObjects()
                .flatMap(objectTypeDefinitionContext -> objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream())
                .filter(manager::isGrpcField)
                .map(fieldDefinitionContext -> new AbstractMap.SimpleEntry<>(grpcNameUtil.getPackageName(fieldDefinitionContext), new AbstractMap.SimpleEntry<>(manager.getFieldTypeName(fieldDefinitionContext.type()), grpcNameUtil.getTo(fieldDefinitionContext))))
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
    }

    public GrpcQueryDataLoaderBuilder setConfiguration(GraphQLConfig graphQLConfig) {
        this.graphQLConfig = graphQLConfig;
        this.typeManager.setGraphQLConfig(graphQLConfig);
        return this;
    }

    public void writeToFiler(Filer filer) throws IOException {
        this.buildClass().writeTo(filer);
        Logger.info("GrpcQueryDataLoader build success");
    }

    private JavaFile buildClass() {
        TypeSpec typeSpec = buildGrpcQueryDataLoader();
        return JavaFile.builder(graphQLConfig.getHandlerPackageName(), typeSpec).build();
    }

    private TypeSpec buildGrpcQueryDataLoader() {
        TypeSpec.Builder builder = TypeSpec.classBuilder("GrpcQueryDataLoader")
                .addModifiers(Modifier.PUBLIC)
                .superclass(ClassName.get(QueryDataLoader.class))
                .addAnnotation(Dependent.class)
                .addField(
                        FieldSpec.builder(
                                ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(IGraphQLDocumentManager.class)),
                                "manager",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).build()
                )
                .addField(
                        FieldSpec.builder(
                                ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(JsonProvider.class)),
                                "jsonProvider",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).build()
                )
                .addField(
                        FieldSpec.builder(
                                ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get("io.graphoenix.grpc.client", "ChannelManager")),
                                "channelManager",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).build()
                )
                .addMethod(buildConstructor())
                .addMethod(buildDispatchMethod());

        typeMap.keySet().forEach(packageName ->
                builder.addField(
                        FieldSpec.builder(
                                ClassName.get(packageName, "ReactorGraphQLServiceGrpc", "ReactorGraphQLServiceStub"),
                                grpcNameUtil.getGraphQLServiceStubParameterName(packageName),
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).build()
                ).addField(
                        FieldSpec.builder(
                                ParameterizedTypeName.get(Mono.class, String.class),
                                grpcNameUtil.packageNameToUnderline(packageName).concat("_JsonMono"),
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
                .addAnnotation(Inject.class)
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(IGraphQLDocumentManager.class)), "manager")
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(JsonProvider.class)), "jsonProvider")
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get("io.graphoenix.grpc.client", "ChannelManager")), "channelManager")
                .addStatement("this.manager = manager")
                .addStatement("this.jsonProvider = jsonProvider")
                .addStatement("this.channelManager = channelManager");

        this.typeMap.keySet().forEach(packageName ->
                builder.addStatement("this.$L = $T.newReactorStub(channelManager.get().getChannel($S))",
                        grpcNameUtil.getGraphQLServiceStubParameterName(packageName),
                        ClassName.get(packageName, "ReactorGraphQLServiceGrpc"),
                        packageName
                ).addStatement("this.$L = build($S).flatMap(operation -> this.$L.operation($T.newBuilder().setRequest(operation.toString()).build())).map(response -> response.getResponse())",
                        grpcNameUtil.packageNameToUnderline(packageName).concat("_JsonMono"),
                        packageName,
                        grpcNameUtil.getGraphQLServiceStubParameterName(packageName),
                        ClassName.get(packageName, "GraphQLRequest")
                )
        );
        return builder.build();
    }

    private MethodSpec buildDispatchMethod() {
        List<CodeBlock> monoList = new ArrayList<>();
        int index = 0;
        for (String packageName : this.typeMap.keySet()) {
            if (index == 0) {
                monoList.add(CodeBlock.of("return this.$L.flatMap(response -> $T.fromRunnable(() -> addResult($S, response)))",
                        grpcNameUtil.packageNameToUnderline(packageName).concat("_JsonMono"),
                        ClassName.get(Mono.class),
                        packageName));
            } else {
                monoList.add(CodeBlock.of(".then(this.$L.flatMap(response -> $T.fromRunnable(() -> addResult($S, response))))",
                        grpcNameUtil.packageNameToUnderline(packageName).concat("_JsonMono"),
                        ClassName.get(Mono.class),
                        packageName));
            }
            index++;
        }
        CodeBlock codeBlock;
        if (monoList.size() > 0) {
            monoList.add(
                    CodeBlock.of(".then($T.fromSupplier(() -> dispatch(jsonValue.asJsonObject())))",
                            ClassName.get(Mono.class)
                    )
            );
            codeBlock = CodeBlock.join(monoList, System.lineSeparator());
        } else {
            codeBlock = CodeBlock.of("return Mono.empty()");
        }
        return MethodSpec.methodBuilder("load")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(ParameterizedTypeName.get(ClassName.get(Mono.class), ClassName.get(JsonValue.class)))
                .addParameter(ParameterSpec.builder(ClassName.get(JsonValue.class), "jsonValue").build())
                .addStatement(codeBlock)
                .build();
    }
}
