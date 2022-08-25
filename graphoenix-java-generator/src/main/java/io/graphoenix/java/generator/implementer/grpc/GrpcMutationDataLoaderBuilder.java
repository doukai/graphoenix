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
import io.graphoenix.core.operation.ObjectValueWithVariable;
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
import jakarta.json.JsonString;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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
                .addMethod(buildDispatchMethod());

        this.typeMap.keySet().forEach(packageName ->
                builder.addField(
                        FieldSpec.builder(
                                ClassName.get(packageName, "ReactorMutationTypeServiceGrpc", "ReactorMutationTypeServiceStub"),
                                grpcNameUtil.getMutationServiceStubParameterName(packageName),
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).build()
                )
        );
        this.typeMap.forEach((key, value) ->
                value.forEach(
                        tuple2 ->
                                builder.addField(
                                        FieldSpec.builder(
                                                ParameterizedTypeName.get(LinkedHashMap.class, String.class, String.class),
                                                grpcNameUtil.getTypeMethodName(key, tuple2._1()).concat("Map"),
                                                Modifier.PRIVATE,
                                                Modifier.FINAL
                                        ).build()
                                ).addField(
                                        FieldSpec.builder(
                                                ParameterizedTypeName.get(Mono.class, String.class),
                                                grpcNameUtil.getTypeMethodName(key, tuple2._1()).concat("JsonMono"),
                                                Modifier.PRIVATE,
                                                Modifier.FINAL
                                        ).build()
                                )
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
                        grpcNameUtil.getMutationServiceStubParameterName(packageName),
                        ClassName.get(packageName, "ReactorMutationTypeServiceGrpc"),
                        packageName
                )
        );

        this.typeMap.forEach((key, value) ->
                value.forEach(
                        tuple2 ->
                                builder.addStatement("this.$L = new $T<>()",
                                        grpcNameUtil.getTypeMethodName(key, tuple2._1()).concat("Map"),
                                        ClassName.get(LinkedHashMap.class)
                                ).addStatement("this.$L = this.$L.$L($T.newBuilder().setArguments(getListArguments($L.values())).build()).map(response -> response.getJson())",
                                        grpcNameUtil.getTypeMethodName(key, tuple2._1()).concat("JsonMono"),
                                        grpcNameUtil.getMutationServiceStubParameterName(key),
                                        grpcNameUtil.getRpcObjectListMethodName(tuple2._1()).concat("Json"),
                                        ClassName.get(key, grpcNameUtil.getRpcMutationListRequestName(tuple2._1())),
                                        grpcNameUtil.getTypeMethodName(key, tuple2._1()).concat("Map")
                                )
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
        String mapName = grpcNameUtil.getTypeMethodName(packageName, typeName).concat("Map");
        return MethodSpec.methodBuilder(grpcNameUtil.getTypeMethodName(packageName, typeName))
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(Mono.class, JsonValue.class))
                .addParameter(ClassName.get(JsonValue.class), "jsonValue")
                .beginControlFlow("if (jsonValue.getValueType().equals($T.ValueType.OBJECT))", ClassName.get(JsonValue.class))
                .addStatement("$T jsonObject = jsonValue.asJsonObject()", ClassName.get(JsonObject.class))
                .addStatement("$T keyField = jsonObject.get($S)", ClassName.get(JsonValue.class), key)
                .addStatement("final int index")
                .beginControlFlow("if (keyField != null && keyField.getValueType().equals($T.ValueType.STRING))", ClassName.get(JsonValue.class))
                .addStatement("$T key = (($T) keyField).getString()", ClassName.get(String.class), ClassName.get(JsonString.class))
                .beginControlFlow("if ($L.containsKey(key))", mapName)
                .addStatement("index = new $T<>($L.keySet()).indexOf(key)", ClassName.get(ArrayList.class), mapName)
                .nextControlFlow("else")
                .addStatement("$L.put(key, jsonObject.toString())", mapName)
                .addStatement("index = $L.size() - 1", mapName)
                .endControlFlow()
                .nextControlFlow("else")
                .addStatement("$L.put($T.randomUUID().toString(), jsonObject.toString())", mapName, ClassName.get(UUID.class))
                .addStatement("index = $L.size() - 1", mapName)
                .endControlFlow()
                .addStatement("return $L.map(json -> jsonProvider.get().createReader(new $T(json)).readArray()).map(item -> item.get(index))",
                        grpcNameUtil.getTypeMethodName(packageName, typeName).concat("JsonMono"),
                        ClassName.get(StringReader.class)
                )
                .nextControlFlow("else")
                .addStatement("return $T.empty()", ClassName.get(Mono.class))
                .endControlFlow()
                .build();
    }

    private MethodSpec buildTypeFieldMethod(String packageName, String typeName, String key) {
        String mapName = grpcNameUtil.getTypeMethodName(packageName, typeName).concat("Map");
        return MethodSpec.methodBuilder(grpcNameUtil.getTypeMethodName(packageName, typeName))
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(Mono.class, JsonValue.class))
                .addParameter(ClassName.get(ValueWithVariable.class), "valueWithVariable")
                .beginControlFlow("if (valueWithVariable.isObject())")
                .addStatement("$T objectValueWithVariable = valueWithVariable.asObject()", ClassName.get(ObjectValueWithVariable.class))
                .addStatement("$T keyField = objectValueWithVariable.get($S)", ClassName.get(ValueWithVariable.class), key)
                .addStatement("final int index")
                .beginControlFlow("if (keyField != null && keyField.isString())")
                .addStatement("$T key = keyField.asString().getValue()", ClassName.get(String.class))
                .beginControlFlow("if ($L.containsKey(key))", mapName)
                .addStatement("index = new $T<>($L.keySet()).indexOf(key)", ClassName.get(ArrayList.class), mapName)
                .nextControlFlow("else")
                .addStatement("$L.put(key, objectValueWithVariable.toString())", mapName)
                .addStatement("index = $L.size() - 1", mapName)
                .endControlFlow()
                .nextControlFlow("else")
                .addStatement("$L.put($T.randomUUID().toString(), objectValueWithVariable.toString())", mapName, ClassName.get(UUID.class))
                .addStatement("index = $L.size() - 1", mapName)
                .endControlFlow()
                .addStatement("return $L.map(json -> jsonProvider.get().createReader(new $T(json)).readArray()).map(item -> item.get(index))",
                        grpcNameUtil.getTypeMethodName(packageName, typeName).concat("JsonMono"),
                        ClassName.get(StringReader.class)
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
                .beginControlFlow("if (jsonValue.getValueType().equals($T.ValueType.ARRAY))", ClassName.get(JsonValue.class))
                .addStatement("$T jsonArray = jsonValue.asJsonArray()", ClassName.get(JsonArray.class))
                .addStatement("return $T.fromIterable(jsonArray).flatMap(item -> $L(item)).collect($T.toJsonArray())",
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
                .beginControlFlow("if (valueWithVariable.isArray())", ClassName.get(JsonValue.class))
                .addStatement("$T arrayValueWithVariable = valueWithVariable.asArray()", ClassName.get(ArrayValueWithVariable.class))
                .addStatement("return $T.fromIterable(arrayValueWithVariable).flatMap(item -> $L(item)).collect($T.toJsonArray())",
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
        List<AbstractMap.SimpleEntry<String, String>> monoEntryList = this.typeMap.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream()
                        .map(tuple2 -> new AbstractMap.SimpleEntry<>(entry.getKey(), tuple2._1())))
                .collect(Collectors.toList());
        List<CodeBlock> monoList = new ArrayList<>();
        for (int index = 0; index < monoEntryList.size(); index++) {
            String typeMethodName = grpcNameUtil.getTypeMethodName(monoEntryList.get(index).getKey(), monoEntryList.get(index).getValue());
            if (index == 0) {
                monoList.add(
                        CodeBlock.of("return $T.just($L.size() == 0).flatMap(empty -> empty ? Mono.empty() : this.$L.then(Mono.fromRunnable($L::clear)))",
                                ClassName.get(Mono.class),
                                typeMethodName.concat("Map"),
                                typeMethodName.concat("JsonMono"),
                                typeMethodName.concat("Map")
                        )
                );
            } else {
                monoList.add(
                        CodeBlock.of(".then($T.just($L.size() == 0).flatMap(empty -> empty ? Mono.empty() : this.$L.then(Mono.fromRunnable($L::clear))))",
                                ClassName.get(Mono.class),
                                typeMethodName.concat("Map"),
                                typeMethodName.concat("JsonMono"),
                                typeMethodName.concat("Map")
                        )
                );
            }
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
