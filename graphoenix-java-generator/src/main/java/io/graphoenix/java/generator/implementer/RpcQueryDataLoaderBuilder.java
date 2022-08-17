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
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.vavr.Tuple;
import io.vavr.Tuple3;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.json.JsonArray;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static io.graphoenix.core.error.GraphQLErrorType.ARGUMENT_NOT_EXIST;
import static io.graphoenix.core.utils.DocumentUtil.DOCUMENT_UTIL;
import static io.graphoenix.spi.constant.Hammurabi.GRPC_DIRECTIVE_NAME;
import static io.graphoenix.spi.constant.Hammurabi.INTROSPECTION_PREFIX;

@ApplicationScoped
public class RpcQueryDataLoaderBuilder {

    private final TypeManager typeManager;
    private GraphQLConfig graphQLConfig;
    private final Map<String, Map<String, Set<String>>> typeMap;

    @Inject
    public RpcQueryDataLoaderBuilder(IGraphQLDocumentManager manager, TypeManager typeManager) {
        this.typeManager = typeManager;
        this.typeMap = manager.getObjects()
                .flatMap(objectTypeDefinitionContext -> objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream())
                .filter(manager::isGrpcField)
                .map(fieldDefinitionContext -> new AbstractMap.SimpleEntry<>(getPackageName(fieldDefinitionContext), new AbstractMap.SimpleEntry<>(manager.getFieldTypeName(fieldDefinitionContext.type()), getTo(fieldDefinitionContext))))
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

    public RpcQueryDataLoaderBuilder setConfiguration(GraphQLConfig graphQLConfig) {
        this.graphQLConfig = graphQLConfig;
        this.typeManager.setGraphQLConfig(graphQLConfig);
        return this;
    }

    public void writeToFiler(Filer filer) throws IOException {
        this.buildClass().writeTo(filer);
        Logger.info("RpcQueryDataLoader build success");
    }

    private JavaFile buildClass() {
        TypeSpec typeSpec = buildRpcQueryDataLoader();
        return JavaFile.builder(graphQLConfig.getHandlerPackageName(), typeSpec).build();
    }

    private TypeSpec buildRpcQueryDataLoader() {
        TypeSpec.Builder builder = TypeSpec.classBuilder("RpcQueryDataLoader")
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
                                ClassName.get(packageName, "ReactorQueryTypeServiceGrpc", "ReactorQueryTypeServiceStub"),
                                getQueryServiceStubParameterName(packageName),
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).build()
                )
        );
        this.typeMap.forEach((packageName, typeNameMap) ->
                typeNameMap.forEach(
                        (typeName, fieldNameList) ->
                                fieldNameList.forEach(fieldName ->
                                        builder.addField(
                                                FieldSpec.builder(
                                                        ParameterizedTypeName.get(LinkedHashSet.class, String.class),
                                                        getTypeMethodName(packageName, typeName, fieldName).concat("Set"),
                                                        Modifier.PRIVATE,
                                                        Modifier.FINAL
                                                ).build()
                                        ).addField(
                                                FieldSpec.builder(
                                                        ParameterizedTypeName.get(Mono.class, JsonArray.class),
                                                        getTypeMethodName(packageName, typeName, fieldName).concat("JsonMono"),
                                                        Modifier.PRIVATE,
                                                        Modifier.FINAL
                                                ).build()
                                        )
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

        typeMap.keySet().forEach(packageName ->
                builder.addStatement("this.$L = $T.newReactorStub(channelManager.get().getChannel($S))",
                        getQueryServiceStubParameterName(packageName),
                        ClassName.get(packageName, "ReactorQueryTypeServiceGrpc"),
                        packageName
                )
        );

        this.typeMap.forEach((packageName, typeNameMap) ->
                typeNameMap.forEach(
                        (typeName, fieldNameList) ->
                                fieldNameList.forEach(fieldName ->
                                        builder.addStatement("this.$L = new $T<>()",
                                                getTypeMethodName(packageName, typeName, fieldName).concat("Set"),
                                                ClassName.get(LinkedHashSet.class)
                                        ).addStatement("this.$L = this.$L.$L($T.newBuilder().$L($T.newBuilder().addAllIn($L)).build()).map(response -> jsonProvider.get().createReader(new $T(response.getJson())).readArray())",
                                                getTypeMethodName(packageName, typeName, fieldName).concat("JsonMono"),
                                                getQueryServiceStubParameterName(packageName),
                                                getRpcObjectListMethodName(typeName).concat("Json"),
                                                ClassName.get(packageName, getRpcQueryListRequestName(typeName)),
                                                getRpcFieldExpressionSetterName(fieldName),
                                                ClassName.get(packageName, "StringExpression"),
                                                getTypeMethodName(packageName, typeName, fieldName).concat("Set"),
                                                ClassName.get(StringReader.class)
                                        )
                                )
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
        return MethodSpec.methodBuilder(getTypeMethodName(packageName, typeName, fieldName))
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(Mono.class, JsonValue.class))
                .addParameter(String.class, "key")
                .addStatement("$L.add(key)", getTypeMethodName(packageName, typeName, fieldName).concat("Set"))
                .addStatement("return $L.map(array -> array.stream().filter(item -> item.asJsonObject().getString($S).equals(key)).findFirst().orElse($T.NULL))",
                        getTypeMethodName(packageName, typeName, fieldName).concat("JsonMono"),
                        fieldName,
                        ClassName.get(JsonValue.class)
                )
                .build();
    }

    private MethodSpec buildTypeListMethod(String packageName, String typeName, String fieldName) {
        return MethodSpec.methodBuilder(getTypeListMethodName(packageName, typeName, fieldName))
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(Mono.class, JsonValue.class))
                .addParameter(String.class, "key")
                .addStatement("$L.add(key)", getTypeMethodName(packageName, typeName, fieldName).concat("Set"))
                .addStatement("return $L.map(array -> array.stream().filter(item -> item.asJsonObject().getString($S).equals(key)).collect($T.toJsonArray()))",
                        getTypeMethodName(packageName, typeName, fieldName).concat("JsonMono"),
                        fieldName,
                        ClassName.get(JsonCollectors.class)
                )
                .build();
    }

    private MethodSpec buildDispatchMethod() {
        List<Tuple3<String, String, String>> monoTupleList = this.typeMap.entrySet().stream()
                .flatMap(packageNameEntry ->
                        packageNameEntry.getValue().entrySet().stream()
                                .flatMap(typeNameEntry ->
                                        typeNameEntry.getValue().stream()
                                                .map(fieldName ->
                                                        Tuple.of(packageNameEntry.getKey(), typeNameEntry.getKey(), fieldName)
                                                )
                                )
                )
                .collect(Collectors.toList());
        List<CodeBlock> monoList = new ArrayList<>();
        for (int index = 0; index < monoTupleList.size(); index++) {
            if (index == 0) {
                monoList.add(CodeBlock.of("return this.$L", getTypeMethodName(monoTupleList.get(index)._1(), monoTupleList.get(index)._2(), monoTupleList.get(index)._3()).concat("JsonMono")));
            } else {
                monoList.add(CodeBlock.of(".then(this.$L)", getTypeMethodName(monoTupleList.get(index)._1(), monoTupleList.get(index)._2(), monoTupleList.get(index)._3()).concat("JsonMono")));
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

    private String getQueryServiceStubParameterName(String packageName) {
        return packageNameToUnderline(packageName).concat("_QueryTypeServiceStub");
    }

    private String getRpcObjectListMethodName(String name) {
        if (name.startsWith(INTROSPECTION_PREFIX)) {
            return "intro".concat(name.replaceFirst(INTROSPECTION_PREFIX, "")).concat("List");
        }
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, name).concat("List");
    }

    private String getRpcFieldExpressionSetterName(String fieldName) {
        if (fieldName.startsWith(INTROSPECTION_PREFIX)) {
            return "setIntro".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, fieldName.replaceFirst(INTROSPECTION_PREFIX, "")));
        }
        return "set".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, fieldName));
    }

    private String getRpcQueryListRequestName(String name) {
        if (name.startsWith(INTROSPECTION_PREFIX)) {
            return "QueryIntro".concat(name.replaceFirst(INTROSPECTION_PREFIX, "")).concat("ListRequest");
        }
        return "Query".concat(name).concat("ListRequest");
    }

    private String getTypeMethodName(String packageName, String typeName, String fieldName) {
        return packageNameToUnderline(packageName).concat("_").concat(typeName).concat("_").concat(fieldName);
    }

    private String getTypeListMethodName(String packageName, String typeName, String fieldName) {
        return getTypeMethodName(packageName, typeName, fieldName).concat("List");
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

    private String getTo(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return fieldDefinitionContext.directives().directive().stream()
                .filter(directiveContext -> directiveContext.name().getText().equals(GRPC_DIRECTIVE_NAME))
                .flatMap(directiveContext -> directiveContext.arguments().argument().stream())
                .filter(argumentContext -> argumentContext.name().getText().equals("to"))
                .filter(argumentContext -> argumentContext.valueWithVariable().StringValue() != null)
                .map(argumentContext -> DOCUMENT_UTIL.getStringValue(argumentContext.valueWithVariable().StringValue()))
                .findFirst()
                .orElseThrow(() -> new GraphQLErrors(ARGUMENT_NOT_EXIST.bind("to")));
    }
}
