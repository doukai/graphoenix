package io.graphoenix.java.generator.implementer;

import com.google.common.base.CaseFormat;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.core.operation.ArrayValueWithVariable;
import io.graphoenix.core.operation.ObjectValueWithVariable;
import io.graphoenix.core.operation.ValueWithVariable;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
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

import static io.graphoenix.core.error.GraphQLErrorType.ARGUMENT_NOT_EXIST;
import static io.graphoenix.core.utils.DocumentUtil.DOCUMENT_UTIL;
import static io.graphoenix.spi.constant.Hammurabi.GRPC_DIRECTIVE_NAME;
import static io.graphoenix.spi.constant.Hammurabi.INTROSPECTION_PREFIX;

@ApplicationScoped
public class RpcMutationDataLoaderBuilder {

    private final TypeManager typeManager;
    private GraphQLConfig graphQLConfig;
    private final Map<String, Set<Tuple2<String, String>>> typeMap;

    @Inject
    public RpcMutationDataLoaderBuilder(IGraphQLDocumentManager manager, TypeManager typeManager) {
        this.typeManager = typeManager;
        this.typeMap = manager.getObjects()
                .flatMap(objectTypeDefinitionContext -> objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream())
                .filter(manager::isGrpcField)
                .map(fieldDefinitionContext -> new AbstractMap.SimpleEntry<>(getPackageName(fieldDefinitionContext), Tuple.of(manager.getFieldTypeName(fieldDefinitionContext.type()), getKey(fieldDefinitionContext))))
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

    public RpcMutationDataLoaderBuilder setConfiguration(GraphQLConfig graphQLConfig) {
        this.graphQLConfig = graphQLConfig;
        this.typeManager.setGraphQLConfig(graphQLConfig);
        return this;
    }

    public void writeToFiler(Filer filer) throws IOException {
        this.buildClass().writeTo(filer);
        Logger.info("RpcMutationDataLoader build success");
    }

    private JavaFile buildClass() {
        TypeSpec typeSpec = buildRpcMutationDataLoader();
        return JavaFile.builder(graphQLConfig.getHandlerPackageName(), typeSpec).build();
    }

    private TypeSpec buildRpcMutationDataLoader() {
        TypeSpec.Builder builder = TypeSpec.classBuilder("RpcMutationDataLoader")
                .superclass(ClassName.get("io.graphoenix.grpc.client", "GrpcBaseDataLoader"))
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Dependent.class)
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
                                getMutationServiceStubParameterName(packageName),
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
                                                getTypeMethodName(key, tuple2._1()).concat("Map"),
                                                Modifier.PRIVATE,
                                                Modifier.FINAL
                                        ).build()
                                ).addField(
                                        FieldSpec.builder(

                                                ParameterizedTypeName.get(Mono.class, String.class),
                                                getTypeMethodName(key, tuple2._1()).concat("JsonMono"),
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
                        getMutationServiceStubParameterName(packageName),
                        ClassName.get(packageName, "ReactorMutationTypeServiceGrpc"),
                        packageName
                )
        );

        this.typeMap.forEach((key, value) ->
                value.forEach(
                        tuple2 ->
                                builder.addStatement("this.$L = new $T<>()",
                                        getTypeMethodName(key, tuple2._1()).concat("Map"),
                                        ClassName.get(LinkedHashMap.class)
                                ).addStatement("this.$L = this.$L.$L($T.newBuilder().setArguments(getListArguments($L.values())).build()).map(response -> response.getJson())",
                                        getTypeMethodName(key, tuple2._1()).concat("JsonMono"),
                                        getMutationServiceStubParameterName(key),
                                        getRpcObjectListMethodName(tuple2._1()).concat("Json"),
                                        ClassName.get(key, getRpcMutationListRequestName(tuple2._1())),
                                        getTypeMethodName(key, tuple2._1()).concat("Map")
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
        String mapName = getTypeMethodName(packageName, typeName).concat("Map");
        return MethodSpec.methodBuilder(getTypeMethodName(packageName, typeName))
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
                        getTypeMethodName(packageName, typeName).concat("JsonMono"),
                        ClassName.get(StringReader.class)
                )
                .nextControlFlow("else")
                .addStatement("return $T.empty()", ClassName.get(Mono.class))
                .endControlFlow()
                .build();
    }

    private MethodSpec buildTypeFieldMethod(String packageName, String typeName, String key) {
        String mapName = getTypeMethodName(packageName, typeName).concat("Map");
        return MethodSpec.methodBuilder(getTypeMethodName(packageName, typeName))
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
                        getTypeMethodName(packageName, typeName).concat("JsonMono"),
                        ClassName.get(StringReader.class)
                )
                .nextControlFlow("else")
                .addStatement("return $T.empty()", ClassName.get(Mono.class))
                .endControlFlow()
                .build();
    }

    private MethodSpec buildTypeListMethod(String packageName, String typeName) {
        return MethodSpec.methodBuilder(getTypeMethodName(packageName, typeName).concat("List"))
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(Mono.class, JsonValue.class))
                .addParameter(ClassName.get(JsonValue.class), "jsonValue")
                .beginControlFlow("if (jsonValue.getValueType().equals($T.ValueType.ARRAY))", ClassName.get(JsonValue.class))
                .addStatement("$T jsonArray = jsonValue.asJsonArray()", ClassName.get(JsonArray.class))
                .addStatement("return $T.fromIterable(jsonArray).flatMap(item -> $L(item)).collect($T.toJsonArray())",
                        ClassName.get(Flux.class),
                        getTypeMethodName(packageName, typeName),
                        ClassName.get(JsonCollectors.class)
                )
                .nextControlFlow("else")
                .addStatement("return $T.empty()", ClassName.get(Mono.class))
                .endControlFlow()
                .build();
    }

    private MethodSpec buildTypeListFieldMethod(String packageName, String typeName) {
        return MethodSpec.methodBuilder(getTypeMethodName(packageName, typeName).concat("List"))
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(Mono.class, JsonValue.class))
                .addParameter(ClassName.get(ValueWithVariable.class), "valueWithVariable")
                .beginControlFlow("if (valueWithVariable.isArray())", ClassName.get(JsonValue.class))
                .addStatement("$T arrayValueWithVariable = valueWithVariable.asArray()", ClassName.get(ArrayValueWithVariable.class))
                .addStatement("return $T.fromIterable(arrayValueWithVariable).flatMap(item -> $L(item)).collect($T.toJsonArray())",
                        ClassName.get(Flux.class),
                        getTypeMethodName(packageName, typeName),
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

            if (index == 0) {
                monoList.add(CodeBlock.of("return this.$L", getTypeMethodName(monoEntryList.get(index).getKey(), monoEntryList.get(index).getValue()).concat("JsonMono")));
            } else {
                monoList.add(CodeBlock.of(".then(this.$L)", getTypeMethodName(monoEntryList.get(index).getKey(), monoEntryList.get(index).getValue()).concat("JsonMono")));
            }
        }
        CodeBlock codeBlock;
        if (monoList.size() > 0) {
            monoList.add(CodeBlock.of(".then()"));
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

    private String getRpcObjectName(String name) {
        if (name.startsWith(INTROSPECTION_PREFIX)) {
            return "Intro".concat(name.replaceFirst(INTROSPECTION_PREFIX, ""));
        }
        return name;
    }

    private String getMutationServiceStubParameterName(String packageName) {
        return packageNameToUnderline(packageName).concat("_MutationTypeServiceStub");
    }

    private String getRpcObjectListMethodName(String name) {
        if (name.startsWith(INTROSPECTION_PREFIX)) {
            return "intro".concat(name.replaceFirst(INTROSPECTION_PREFIX, "")).concat("List");
        }
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, name).concat("List");
    }

    private String getRpcMutationListRequestName(String name) {
        if (name.startsWith(INTROSPECTION_PREFIX)) {
            return "MutationIntro".concat(name.replaceFirst(INTROSPECTION_PREFIX, "")).concat("ListRequest");
        }
        return "Mutation".concat(name).concat("ListRequest");
    }

    private String getRpcResponseListMethodName(String name) {
        if (name.startsWith(INTROSPECTION_PREFIX)) {
            return "getIntro".concat(name.replaceFirst(INTROSPECTION_PREFIX, "")).concat("ListList");
        }
        return "get".concat(name).concat("ListList");
    }

    private String getTypeMethodName(String packageName, String typeName) {
        return packageNameToUnderline(packageName).concat("_").concat(typeName);
    }

    private String packageNameToUnderline(String packageName) {
        return String.join("_", packageName.split("\\."));
    }

    private String getPackageName(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return fieldDefinitionContext.directives().directive().stream()
                .filter(directiveContext -> directiveContext.name().getText().equals(GRPC_DIRECTIVE_NAME))
                .flatMap(directiveContext -> directiveContext.arguments().argument().stream())
                .filter(argumentContext -> argumentContext.name().getText().equals("packageName"))
                .filter(argumentContext -> argumentContext.valueWithVariable().StringValue() != null)
                .map(argumentContext -> DOCUMENT_UTIL.getStringValue(argumentContext.valueWithVariable().StringValue()))
                .findFirst()
                .orElseThrow(() -> new GraphQLErrors(ARGUMENT_NOT_EXIST.bind("packageName")));
    }

    private String getKey(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return fieldDefinitionContext.directives().directive().stream()
                .filter(directiveContext -> directiveContext.name().getText().equals(GRPC_DIRECTIVE_NAME))
                .flatMap(directiveContext -> directiveContext.arguments().argument().stream())
                .filter(argumentContext -> argumentContext.name().getText().equals("key"))
                .filter(argumentContext -> argumentContext.valueWithVariable().StringValue() != null)
                .map(argumentContext -> DOCUMENT_UTIL.getStringValue(argumentContext.valueWithVariable().StringValue()))
                .findFirst()
                .orElseThrow(() -> new GraphQLErrors(ARGUMENT_NOT_EXIST.bind("key")));
    }
}
