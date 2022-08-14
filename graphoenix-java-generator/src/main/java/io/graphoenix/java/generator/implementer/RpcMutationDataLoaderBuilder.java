package io.graphoenix.java.generator.implementer;

import com.google.common.base.CaseFormat;
import com.squareup.javapoet.*;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.tinylog.Logger;
import reactor.core.publisher.Mono;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static io.graphoenix.core.error.GraphQLErrorType.ARGUMENT_NOT_EXIST;
import static io.graphoenix.core.utils.DocumentUtil.DOCUMENT_UTIL;
import static io.graphoenix.spi.constant.Hammurabi.GRPC_DIRECTIVE_NAME;
import static io.graphoenix.spi.constant.Hammurabi.INTROSPECTION_PREFIX;

@ApplicationScoped
public class RpcMutationDataLoaderBuilder {

    private final TypeManager typeManager;
    private GraphQLConfig graphQLConfig;
    private final Map<String, Set<String>> typeMap;

    @Inject
    public RpcMutationDataLoaderBuilder(IGraphQLDocumentManager manager, TypeManager typeManager) {
        this.typeManager = typeManager;
        this.typeMap = manager.getObjects()
                .flatMap(objectTypeDefinitionContext -> objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream())
                .filter(manager::isGrpcField)
                .map(fieldDefinitionContext -> new AbstractMap.SimpleEntry<>(getPackageName(fieldDefinitionContext), manager.getFieldTypeName(fieldDefinitionContext.type())))
                .collect(
                        Collectors.groupingBy(
                                AbstractMap.SimpleEntry<String, String>::getKey,
                                Collectors.mapping(
                                        AbstractMap.SimpleEntry<String, String>::getValue,
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
                        typeName ->
                                builder.addField(
                                        FieldSpec.builder(
                                                ParameterizedTypeName.get(LinkedHashMap.class, String.class, String.class),
                                                getTypeMethodName(key, typeName).concat("Map"),
                                                Modifier.PRIVATE,
                                                Modifier.FINAL
                                        ).build()
                                ).addField(
                                        FieldSpec.builder(
                                                ParameterizedTypeName.get(ClassName.get(Mono.class), ParameterizedTypeName.get(ClassName.get(List.class), ClassName.get(key, getRpcObjectName(typeName)))),
                                                getTypeMethodName(key, typeName).concat("ListMono"),
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
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get("io.graphoenix.grpc.client", "ChannelManager")), "channelManager")
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
                        typeName ->
                                builder.addStatement("this.$L = new $T<>()",
                                        getTypeMethodName(key, typeName).concat("Map"),
                                        ClassName.get(LinkedHashMap.class)
                                ).addStatement("this.$L = this.$L.$L($T.newBuilder().setArguments(getListArguments($L.values())).build()).map(response -> response.$L())",
                                        getTypeMethodName(key, typeName).concat("ListMono"),
                                        getMutationServiceStubParameterName(key),
                                        getRpcObjectListMethodName(typeName),
                                        ClassName.get(key, getRpcMutationListRequestName(typeName)),
                                        getTypeMethodName(key, typeName).concat("Map"),
                                        getRpcResponseListMethodName(typeName)
                                )
                )
        );
        return builder.build();
    }

    private List<MethodSpec> buildTypeMethods() {
        return this.typeMap.entrySet().stream()
                .flatMap(entry ->
                        entry.getValue().stream()
                                .map(typeName -> buildTypeMethod(entry.getKey(), typeName))
                )
                .collect(Collectors.toList());
    }

    private MethodSpec buildTypeMethod(String packageName, String typeName) {
        return MethodSpec.methodBuilder(getTypeMethodName(packageName, typeName))
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(ClassName.get(Mono.class), ClassName.get(packageName, getRpcObjectName(typeName))))
                .addParameter(String.class, "key")
                .addParameter(ClassName.get(GraphqlParser.ValueWithVariableContext.class), "valueWithVariableContext")
                .beginControlFlow("if (!$L.containsKey(key))", getTypeMethodName(packageName, typeName).concat("Map"))
                .addStatement("$L.put(key, valueWithVariableToString(valueWithVariableContext))", getTypeMethodName(packageName, typeName).concat("Map"))
                .endControlFlow()
                .addStatement("final int index = new $T($L.keySet()).indexOf(key)", ClassName.get(ArrayList.class), getTypeMethodName(packageName, typeName).concat("Map"))
                .addStatement("return $L.map(item -> item.get(index))", getTypeMethodName(packageName, typeName).concat("ListMono"))
                .build();
    }

    private MethodSpec buildDispatchMethod() {
        List<AbstractMap.SimpleEntry<String, String>> monoEntryList = this.typeMap.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream()
                        .map(typeName -> new AbstractMap.SimpleEntry<>(entry.getKey(), typeName)))
                .collect(Collectors.toList());
        List<CodeBlock> monoList = new ArrayList<>();
        for (int index = 0; index < monoEntryList.size(); index++) {

            if (index == 0) {
                monoList.add(CodeBlock.of("return this.$L", getTypeMethodName(monoEntryList.get(index).getKey(), monoEntryList.get(index).getValue()).concat("ListMono")));
            } else {
                monoList.add(CodeBlock.of(".then(this.$L)", getTypeMethodName(monoEntryList.get(index).getKey(), monoEntryList.get(index).getValue()).concat("ListMono")));
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
}
