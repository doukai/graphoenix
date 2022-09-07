package io.graphoenix.java.generator.implementer.grpc;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.operation.ArrayValueWithVariable;
import io.graphoenix.core.operation.ValueWithVariable;
import io.graphoenix.java.generator.implementer.TypeManager;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.vavr.Tuple;
import io.vavr.Tuple2;
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
import reactor.core.publisher.Flux;
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
public class GrpcMutationDataLoaderBuilder {

    private final TypeManager typeManager;
    private GraphQLConfig graphQLConfig;
    private final GrpcNameUtil grpcNameUtil;
    private final Map<String, Set<Tuple2<String, String>>> typeMap;

    @Inject
    public GrpcMutationDataLoaderBuilder(IGraphQLDocumentManager manager, TypeManager typeManager, GrpcNameUtil grpcNameUtil) {
        this.typeManager = typeManager;
        this.grpcNameUtil = grpcNameUtil;
        this.typeMap = manager.getObjects()
                .flatMap(objectTypeDefinitionContext -> objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream())
                .filter(manager::isGrpcField)
                .map(fieldDefinitionContext -> new AbstractMap.SimpleEntry<>(grpcNameUtil.getPackageName(fieldDefinitionContext), Tuple.of(manager.getFieldTypeName(fieldDefinitionContext.type()), grpcNameUtil.getKey(fieldDefinitionContext))))
                .collect(
                        Collectors.groupingBy(
                                AbstractMap.SimpleEntry<String, Tuple2<String, String>>::getKey,
                                Collectors.mapping(
                                        AbstractMap.SimpleEntry<String, Tuple2<String, String>>::getValue,
                                        Collectors.toSet()
                                )
                        )
                );
    }

    public GrpcMutationDataLoaderBuilder setConfiguration(GraphQLConfig graphQLConfig) {
        this.graphQLConfig = graphQLConfig;
        this.typeManager.setGraphQLConfig(graphQLConfig);
        return this;
    }

    public void writeToFiler(Filer filer) throws IOException {
        this.buildClass().writeTo(filer);
        Logger.info("GrpcMutationDataLoader build success");
    }

    private JavaFile buildClass() {
        TypeSpec typeSpec = buildGrpcMutationDataLoader();
        return JavaFile.builder(graphQLConfig.getHandlerPackageName(), typeSpec).build();
    }

    private TypeSpec buildGrpcMutationDataLoader() {
        TypeSpec.Builder builder = TypeSpec.classBuilder("GrpcMutationDataLoader")
                .superclass(ClassName.get("io.graphoenix.grpc.client", "GrpcBaseMutationDataLoader"))
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
                .addMethod(buildDispatchMethod());

        this.typeMap.keySet().forEach(packageName ->
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
                ).addStatement("this.$L = build($S).flatMap(operation -> this.$L.operation($T.newBuilder().setRequest(operation.toString()).build())).map(response -> jsonProvider.get().createReader(new $T(response.getResponse())).readObject().get($S).asJsonObject())",
                        grpcNameUtil.packageNameToUnderline(packageName).concat("_JsonMono"),
                        packageName,
                        grpcNameUtil.getGraphQLServiceStubParameterName(packageName),
                        ClassName.get(packageName, "GraphQLRequest"),
                        ClassName.get(StringReader.class),
                        "data"
                )
        );
        return builder.build();
    }

    private List<MethodSpec> buildTypeMethods() {
        return this.typeMap.entrySet().stream()
                .flatMap(entry ->
                        entry.getValue().stream()
                                .flatMap(tuple2 ->
                                        Stream.of(
                                                buildTypeMethod(entry.getKey(), tuple2._1(), tuple2._2()),
                                                buildTypeFieldMethod(entry.getKey(), tuple2._1(), tuple2._2()),
                                                buildTypeListMethod(entry.getKey(), tuple2._1()),
                                                buildTypeListFieldMethod(entry.getKey(), tuple2._1())
                                        )
                                )
                )
                .collect(Collectors.toList());
    }

    private MethodSpec buildTypeMethod(String packageName, String typeName, String key) {
        return MethodSpec.methodBuilder(grpcNameUtil.getTypeMethodName(packageName, typeName))
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(Mono.class, JsonValue.class))
                .addParameter(ClassName.get(JsonValue.class), "jsonValue")
                .addParameter(ClassName.get(String.class), "selectionName")
                .beginControlFlow("if (jsonValue.getValueType().equals($T.ValueType.OBJECT))", ClassName.get(JsonValue.class))
                .addStatement("final int index = addObjectValue($S, $S, jsonValue.asJsonObject(), $S)", packageName, typeName, key)
                .addStatement("addSelection($S, $S, selectionName)", packageName, typeName)
                .addStatement("return $L.map(jsonObject -> jsonObject.getJsonArray(typeToLowerCamelName($S)).get(index).asJsonObject().getOrDefault(selectionName, $T.NULL))",
                        grpcNameUtil.packageNameToUnderline(packageName).concat("_JsonMono"),
                        typeName,
                        ClassName.get(JsonValue.class)
                )
                .nextControlFlow("else")
                .addStatement("return $T.empty()", ClassName.get(Mono.class))
                .endControlFlow()
                .build();
    }

    private MethodSpec buildTypeFieldMethod(String packageName, String typeName, String key) {
        return MethodSpec.methodBuilder(grpcNameUtil.getTypeMethodName(packageName, typeName))
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(Mono.class, JsonValue.class))
                .addParameter(ClassName.get(ValueWithVariable.class), "valueWithVariable")
                .addParameter(ClassName.get(String.class), "selectionName")
                .beginControlFlow("if (valueWithVariable.isObject())")
                .addStatement("final int index = addObjectValue($S, $S, valueWithVariable.asObject(), $S)", packageName, typeName, key)
                .addStatement("addSelection($S, $S, selectionName)", packageName, typeName)
                .addStatement("return $L.map(jsonObject -> jsonObject.getJsonArray(typeToLowerCamelName($S)).get(index).asJsonObject().getOrDefault(selectionName, $T.NULL))",
                        grpcNameUtil.packageNameToUnderline(packageName).concat("_JsonMono"),
                        typeName,
                        ClassName.get(JsonValue.class)
                )
                .nextControlFlow("else")
                .addStatement("return $T.empty()", ClassName.get(Mono.class))
                .endControlFlow()
                .build();
    }

    private MethodSpec buildTypeListMethod(String packageName, String typeName) {
        return MethodSpec.methodBuilder(grpcNameUtil.getTypeMethodName(packageName, typeName).concat("List"))
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(Mono.class, JsonValue.class))
                .addParameter(ClassName.get(JsonValue.class), "jsonValue")
                .addParameter(ClassName.get(String.class), "selectionName")
                .beginControlFlow("if (jsonValue.getValueType().equals($T.ValueType.ARRAY))", ClassName.get(JsonValue.class))
                .addStatement("$T jsonArray = jsonValue.asJsonArray()", ClassName.get(JsonArray.class))
                .addStatement("return $T.fromIterable(jsonArray).flatMap(item -> $L(item, selectionName)).collect($T.toJsonArray())",
                        ClassName.get(Flux.class),
                        grpcNameUtil.getTypeMethodName(packageName, typeName),
                        ClassName.get(JsonCollectors.class)
                )
                .nextControlFlow("else")
                .addStatement("return $T.empty()", ClassName.get(Mono.class))
                .endControlFlow()
                .build();
    }

    private MethodSpec buildTypeListFieldMethod(String packageName, String typeName) {
        return MethodSpec.methodBuilder(grpcNameUtil.getTypeMethodName(packageName, typeName).concat("List"))
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(Mono.class, JsonValue.class))
                .addParameter(ClassName.get(ValueWithVariable.class), "valueWithVariable")
                .addParameter(ClassName.get(String.class), "selectionName")
                .beginControlFlow("if (valueWithVariable.isArray())", ClassName.get(JsonValue.class))
                .addStatement("$T arrayValueWithVariable = valueWithVariable.asArray()", ClassName.get(ArrayValueWithVariable.class))
                .addStatement("return $T.fromIterable(arrayValueWithVariable).flatMap(item -> $L(item, selectionName)).collect($T.toJsonArray())",
                        ClassName.get(Flux.class),
                        grpcNameUtil.getTypeMethodName(packageName, typeName),
                        ClassName.get(JsonCollectors.class)
                )
                .nextControlFlow("else")
                .addStatement("return $T.empty()", ClassName.get(Mono.class))
                .endControlFlow()
                .build();
    }

    private MethodSpec buildDispatchMethod() {
        List<CodeBlock> monoList = new ArrayList<>();
        int index = 0;
        for (String packageName : this.typeMap.keySet()) {
            if (index == 0) {
                monoList.add(CodeBlock.of("return this.$L", grpcNameUtil.packageNameToUnderline(packageName).concat("_JsonMono")));
            } else {
                monoList.add(CodeBlock.of(".then(this.$L)", grpcNameUtil.packageNameToUnderline(packageName).concat("_JsonMono")));
            }
            index++;
        }
        if (monoList.size() == 1) {
            monoList.add(CodeBlock.of(".then()"));
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
