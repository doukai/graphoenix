package io.graphoenix.java.generator.implementer.grpc;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.java.generator.implementer.TypeManager;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.json.spi.JsonProvider;
import jakarta.json.stream.JsonCollectors;
import org.tinylog.Logger;
import reactor.core.publisher.Mono;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.io.StringReader;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
public class GrpcQueryDataLoaderBuilder {

    private final TypeManager typeManager;
    private GraphQLConfig graphQLConfig;
    private final GrpcNameUtil grpcNameUtil;
    private final Map<String, Map<String, Set<String>>> typeMap;

    @Inject
    public GrpcQueryDataLoaderBuilder(IGraphQLDocumentManager manager, TypeManager typeManager, GrpcNameUtil grpcNameUtil) {
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
                .superclass(ClassName.get("io.graphoenix.grpc.client", "GrpcBaseDataLoader"))
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(RequestScoped.class)
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
                .addMethods(buildTypeMethods())
                .addMethods(buildTypeListMethods())
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
                                ParameterizedTypeName.get(Mono.class, JsonObject.class),
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
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(JsonProvider.class)), "jsonProvider")
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get("io.graphoenix.grpc.client", "ChannelManager")), "channelManager")
                .addStatement("this.jsonProvider = jsonProvider")
                .addStatement("this.channelManager = channelManager");

        this.typeMap.keySet().forEach(packageName ->
                builder.addStatement("this.$L = $T.newReactorStub(channelManager.get().getChannel($S))",
                        grpcNameUtil.getGraphQLServiceStubParameterName(packageName),
                        ClassName.get(packageName, "ReactorGraphQLServiceGrpc"),
                        packageName
                ).addStatement("this.$L = this.$L.operation($T.newBuilder().setRequest(buildOperation($S).toString()).build()).map(response -> jsonProvider.get().createReader(new $T(response.getResponse())).readObject().get($S).asJsonObject())",
                        grpcNameUtil.packageNameToUnderline(packageName).concat("_JsonMono"),
                        grpcNameUtil.getGraphQLServiceStubParameterName(packageName),
                        ClassName.get(packageName, "GraphQLRequest"),
                        packageName,
                        ClassName.get(StringReader.class),
                        "data"
                )
        );
        return builder.build();
    }

    private List<MethodSpec> buildTypeMethods() {
        return this.typeMap.entrySet().stream()
                .flatMap(packageNameEntry ->
                        packageNameEntry.getValue().entrySet().stream()
                                .flatMap(typeNameEntry ->
                                        typeNameEntry.getValue().stream()
                                                .map(fieldName ->
                                                        buildTypeMethod(packageNameEntry.getKey(), typeNameEntry.getKey(), fieldName)
                                                )
                                )
                )
                .collect(Collectors.toList());
    }

    private List<MethodSpec> buildTypeListMethods() {
        return this.typeMap.entrySet().stream()
                .flatMap(packageNameEntry ->
                        packageNameEntry.getValue().entrySet().stream()
                                .flatMap(typeNameEntry ->
                                        typeNameEntry.getValue().stream()
                                                .map(fieldName ->
                                                        buildTypeListMethod(packageNameEntry.getKey(), typeNameEntry.getKey(), fieldName)
                                                )
                                )
                )
                .collect(Collectors.toList());
    }

    private MethodSpec buildTypeMethod(String packageName, String typeName, String fieldName) {
        return MethodSpec.methodBuilder(grpcNameUtil.getTypeMethodName(packageName, typeName, fieldName))
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(Mono.class, JsonValue.class))
                .addParameter(String.class, "key")
                .addParameter(GraphqlParser.SelectionSetContext.class, "selectionSetContext")
                .addStatement("mergeSelection($S, $S, selectionSetContext)", packageName, typeName)
                .addStatement("addCondition($S, $S, $S, key)", packageName, typeName, fieldName)
                .addStatement("return $L.map(jsonObject -> $T.ofNullable(jsonObject.getJsonArray(getQueryFieldAlias($S, $S))).flatMap($T::stream).filter(item -> item.asJsonObject().getString($S).equals(key)).findFirst().orElse($T.NULL))",
                        grpcNameUtil.packageNameToUnderline(packageName).concat("_JsonMono"),
                        ClassName.get(Stream.class),
                        typeName,
                        fieldName,
                        ClassName.get(JsonArray.class),
                        fieldName,
                        ClassName.get(JsonValue.class)
                )
                .build();
    }

    private MethodSpec buildTypeListMethod(String packageName, String typeName, String fieldName) {
        return MethodSpec.methodBuilder(grpcNameUtil.getTypeListMethodName(packageName, typeName, fieldName))
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(Mono.class, JsonValue.class))
                .addParameter(String.class, "key")
                .addParameter(GraphqlParser.SelectionSetContext.class, "selectionSetContext")
                .addStatement("mergeSelection($S, $S, selectionSetContext)", packageName, typeName)
                .addStatement("addCondition($S, $S, $S, key)", packageName, typeName, fieldName)
                .addStatement("return $L.map(jsonObject -> $T.ofNullable(jsonObject.getJsonArray(getQueryFieldAlias($S, $S))).flatMap($T::stream).filter(item -> item.asJsonObject().getString($S).equals(key)).collect($T.toJsonArray()))",
                        grpcNameUtil.packageNameToUnderline(packageName).concat("_JsonMono"),
                        ClassName.get(Stream.class),
                        typeName,
                        fieldName,
                        ClassName.get(JsonArray.class),
                        fieldName,
                        ClassName.get(JsonCollectors.class)
                )
                .build();
    }

    private MethodSpec buildDispatchMethod() {
        List<CodeBlock> monoList = new ArrayList<>();
        int index = 0;
        for (String packageName : this.typeMap.keySet()) {
            if (index == 0) {
                monoList.add(
                        CodeBlock.of("return this.$L.then(Mono.fromRunnable(() -> clear($S)))",
                                grpcNameUtil.packageNameToUnderline(packageName).concat("_JsonMono"),
                                packageName
                        )
                );
            } else {
                monoList.add(
                        CodeBlock.of(".then(this.$L.then(Mono.fromRunnable(() -> clear($S))))",
                                grpcNameUtil.packageNameToUnderline(packageName).concat("_JsonMono"),
                                packageName
                        )
                );
            }
            index++;
        }
        CodeBlock codeBlock;
        if (monoList.size() > 0) {
            codeBlock = CodeBlock.join(monoList, System.lineSeparator());
        } else {
            codeBlock = CodeBlock.of("return Mono.empty()");
        }
        return MethodSpec.methodBuilder("dispatch")
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(ClassName.get(Mono.class), ClassName.get(Void.class)))
                .addStatement(codeBlock)
                .build();
    }
}
