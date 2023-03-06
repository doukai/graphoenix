package io.graphoenix.java.generator.implementer;

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

import static io.graphoenix.core.utils.TypeNameUtil.TYPE_NAME_UTIL;

@ApplicationScoped
public class QueryDataLoaderBuilder {

    private final IGraphQLDocumentManager manager;
    private final TypeManager typeManager;
    private GraphQLConfig graphQLConfig;
    private Map<String, Map<String, Map<String, Set<String>>>> fetchTypeMap;

    @Inject
    public QueryDataLoaderBuilder(IGraphQLDocumentManager manager, TypeManager typeManager) {
        this.manager = manager;
        this.typeManager = typeManager;
    }

    public QueryDataLoaderBuilder setConfiguration(GraphQLConfig graphQLConfig) {
        this.graphQLConfig = graphQLConfig;
        this.typeManager.setGraphQLConfig(graphQLConfig);
        return this;
    }

    public void writeToFiler(Filer filer) throws IOException {
        this.buildClass().writeTo(filer);
        Logger.info("QueryDataLoader build success");
    }

    private JavaFile buildClass() {
        fetchTypeMap = manager.getObjects()
                .flatMap(objectTypeDefinitionContext -> objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream())
                .filter(manager::isFetchField)
                .map(fieldDefinitionContext ->
                        new AbstractMap.SimpleEntry<>(
                                manager.getPackageName(manager.getFieldTypeName(fieldDefinitionContext.type())),
                                new AbstractMap.SimpleEntry<>(
                                        manager.getProtocol(fieldDefinitionContext),
                                        new AbstractMap.SimpleEntry<>(
                                                manager.getFieldTypeName(fieldDefinitionContext.type()),
                                                manager.getTo(fieldDefinitionContext)
                                        )
                                )
                        )
                )
                .collect(
                        Collectors.groupingBy(
                                Map.Entry<String, AbstractMap.SimpleEntry<String, AbstractMap.SimpleEntry<String, String>>>::getKey,
                                Collectors.mapping(
                                        Map.Entry<String, AbstractMap.SimpleEntry<String, AbstractMap.SimpleEntry<String, String>>>::getValue,
                                        Collectors.groupingBy(
                                                Map.Entry<String, AbstractMap.SimpleEntry<String, String>>::getKey,
                                                Collectors.mapping(
                                                        Map.Entry<String, AbstractMap.SimpleEntry<String, String>>::getValue,
                                                        Collectors.groupingBy(
                                                                Map.Entry<String, String>::getKey,
                                                                Collectors.mapping(
                                                                        Map.Entry<String, String>::getValue,
                                                                        Collectors.toSet()
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                );

        TypeSpec typeSpec = buildQueryDataLoader();
        return JavaFile.builder(graphQLConfig.getHandlerPackageName(), typeSpec).build();
    }

    private TypeSpec buildQueryDataLoader() {
        TypeSpec.Builder builder = TypeSpec.classBuilder("QueryDataLoaderImpl")
                .addModifiers(Modifier.PUBLIC)
                .superclass(ClassName.get(QueryDataLoader.class))
                .addAnnotation(Dependent.class)
                .addMethod(buildConstructor())
                .addMethod(buildDispatchMethod());

        if (this.fetchTypeMap.size() > 0) {
            builder.addField(
                    FieldSpec.builder(
                            ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get("io.graphoenix.grpc.client", "ChannelManager")),
                            "channelManager",
                            Modifier.PRIVATE,
                            Modifier.FINAL
                    ).build()
            );
        }
        fetchTypeMap.keySet().forEach(packageName ->
                builder.addField(
                        FieldSpec.builder(
                                ClassName.get(packageName, "ReactorGraphQLServiceGrpc", "ReactorGraphQLServiceStub"),
                                fetchTypeNameUtil.getGraphQLServiceStubParameterName(packageName),
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).build()
                ).addField(
                        FieldSpec.builder(
                                ParameterizedTypeName.get(Mono.class, String.class),
                                TYPE_NAME_UTIL.packageNameToUnderline(packageName).concat("_JsonMono"),
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
                .addStatement("this.manager = manager")
                .addStatement("this.jsonProvider = jsonProvider");

        if (this.fetchTypeMap.size() > 0) {
            builder.addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get("io.graphoenix.grpc.client", "ChannelManager")), "channelManager")
                    .addStatement("this.channelManager = channelManager");
        }
        this.fetchTypeMap.keySet().forEach(packageName ->
                builder.addStatement("this.$L = $T.newReactorStub(channelManager.get().getChannel($S))",
                        fetchTypeNameUtil.getGraphQLServiceStubParameterName(packageName),
                        ClassName.get(packageName, "ReactorGraphQLServiceGrpc"),
                        packageName
                ).addStatement("this.$L = build($S).flatMap(operation -> this.$L.operation($T.newBuilder().setRequest(operation.toString()).build())).map(response -> response.getResponse())",
                        fetchTypeNameUtil.packageNameToUnderline(packageName).concat("_JsonMono"),
                        packageName,
                        fetchTypeNameUtil.getGraphQLServiceStubParameterName(packageName),
                        ClassName.get(packageName, "GraphQLRequest")
                )
        );
        return builder.build();
    }

    private MethodSpec buildDispatchMethod() {
        List<CodeBlock> monoList = new ArrayList<>();
        int index = 0;
        for (String packageName : this.fetchTypeMap.keySet()) {
            if (index == 0) {
                monoList.add(CodeBlock.of("return this.$L.flatMap(response -> $T.fromRunnable(() -> addResult($S, response)))",
                        fetchTypeNameUtil.packageNameToUnderline(packageName).concat("_JsonMono"),
                        ClassName.get(Mono.class),
                        packageName));
            } else {
                monoList.add(CodeBlock.of(".then(this.$L.flatMap(response -> $T.fromRunnable(() -> addResult($S, response))))",
                        fetchTypeNameUtil.packageNameToUnderline(packageName).concat("_JsonMono"),
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
