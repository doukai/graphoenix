package io.graphoenix.java.generator.implementer;

import com.google.common.base.CaseFormat;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.tinylog.Logger;
import reactor.core.publisher.Mono;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.graphoenix.core.error.GraphQLErrorType.QUERY_TYPE_NOT_EXIST;
import static io.graphoenix.spi.constant.Hammurabi.AGGREGATE_SUFFIX;
import static io.graphoenix.spi.constant.Hammurabi.INTROSPECTION_PREFIX;

@ApplicationScoped
public class RpcServiceImplementer {

    private final IGraphQLDocumentManager manager;
    private final TypeManager typeManager;
    private GraphQLConfig graphQLConfig;

    @Inject
    public RpcServiceImplementer(IGraphQLDocumentManager manager, TypeManager typeManager) {
        this.manager = manager;
        this.typeManager = typeManager;
    }

    public RpcServiceImplementer setConfiguration(GraphQLConfig graphQLConfig) {
        this.graphQLConfig = graphQLConfig;
        this.typeManager.setGraphQLConfig(graphQLConfig);
        return this;
    }

    public void writeToFiler(Filer filer) throws IOException {
        this.buildQueryTypeServiceImplClass().writeTo(filer);
        Logger.info("QueryTypeServiceImpl build success");
    }

    private JavaFile buildQueryTypeServiceImplClass() {
        TypeSpec typeSpec = buildQueryTypeServiceImpl();
        return JavaFile.builder(graphQLConfig.getHandlerPackageName(), typeSpec).build();
    }

    private TypeSpec buildQueryTypeServiceImpl() {
        return TypeSpec.classBuilder("QueryTypeServiceImpl")
                .superclass(ClassName.get(graphQLConfig.getGrpcPackageName(), "ReactorQueryTypeServiceGrpc", "QueryTypeServiceImplBase"))
                .addModifiers(Modifier.PUBLIC)
                .addField(
                        FieldSpec.builder(
                                ClassName.get(graphQLConfig.getHandlerPackageName(), "RpcRequestHandler"),
                                "requestHandler",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).build()
                )
                .addField(
                        FieldSpec.builder(
                                ClassName.get(String.class),
                                "EMPTY",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).initializer("\"\"").build()
                )
                .addField(
                        FieldSpec.builder(
                                ClassName.get(String.class),
                                "QUERY",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).initializer("\"query\"").build()
                )
                .addField(
                        FieldSpec.builder(
                                ClassName.get(String.class),
                                "MUTATION",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).initializer("\"mutation\"").build()
                )
                .addField(
                        FieldSpec.builder(
                                ClassName.get(String.class),
                                "SPACE",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).initializer("\" \"").build()
                )
                .addField(
                        FieldSpec.builder(
                                ClassName.get(String.class),
                                "COLON",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).initializer("\": \"").build()
                )
                .addField(
                        FieldSpec.builder(
                                ClassName.get(String.class),
                                "COMMA",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).initializer("\", \"").build()
                )
                .addField(
                        FieldSpec.builder(
                                ClassName.get(String.class),
                                "QUOTATION",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).initializer("\"\\\"\"").build()
                )
                .addField(
                        FieldSpec.builder(
                                ClassName.get(String.class),
                                "BRACKETS_START",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).initializer("\"(\"").build()
                )
                .addField(
                        FieldSpec.builder(
                                ClassName.get(String.class),
                                "BRACKETS_END",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).initializer("\")\"").build()
                )
                .addField(
                        FieldSpec.builder(
                                ClassName.get(String.class),
                                "CURLY_BRACKETS_START",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).initializer("\"{\"").build()
                )
                .addField(
                        FieldSpec.builder(
                                ClassName.get(String.class),
                                "CURLY_BRACKETS_END",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).initializer("\"}\"").build()
                )
                .addField(
                        FieldSpec.builder(
                                ClassName.get(String.class),
                                "SQUARE_BRACKETS_START",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).initializer("\"[\"").build()
                )
                .addField(
                        FieldSpec.builder(
                                ClassName.get(String.class),
                                "SQUARE_BRACKETS_END",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).initializer("\"]\"").build()
                )
                .addMethod(buildConstructor())
                .addMethods(buildQueryTypeMethods())
                .build();
    }

    private MethodSpec buildConstructor() {
        return MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addStatement("this.requestHandler = new $T()", ClassName.get(graphQLConfig.getHandlerPackageName(), "RpcRequestHandler"))
                .build();
    }

    private List<MethodSpec> buildQueryTypeMethods() {
        return manager.getQueryOperationTypeName().flatMap(manager::getObject)
                .orElseThrow(() -> new GraphQLErrors(QUERY_TYPE_NOT_EXIST))
                .fieldsDefinition().fieldDefinition().stream()
                .map(this::buildTypeMethod)
                .collect(Collectors.toList());
    }

    private MethodSpec buildTypeMethod(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        String methodName = getRpcName(fieldDefinitionContext);
        String requestParameterName = "request";

        ParameterizedTypeName requestClassName = ParameterizedTypeName.get(ClassName.get(Mono.class), ClassName.get(graphQLConfig.getGrpcPackageName(), getRpcQueryRequestClassName(fieldDefinitionContext)));
        ParameterizedTypeName responseClassName = ParameterizedTypeName.get(ClassName.get(Mono.class), ClassName.get(graphQLConfig.getGrpcPackageName(), getRpcQueryResponseClassName(fieldDefinitionContext)));
        MethodSpec.Builder builder = MethodSpec.methodBuilder(methodName)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(requestClassName, requestParameterName)
                .returns(responseClassName);

        return builder.build();
    }

    private String buildSelectionSet(GraphqlParser.TypeContext typeContext) {
        return buildSelectionSet(typeContext, 0, 0);
    }

    private String buildSelectionSet(GraphqlParser.TypeContext typeContext, int level, int layers) {
        String typeName = manager.getFieldTypeName(typeContext);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{");

        String fieldNames = manager.getFields(typeName)
                .filter(fieldDefinitionContext -> manager.isNotFunctionField(typeName, fieldDefinitionContext.name().getText()))
                .filter(fieldDefinitionContext -> manager.isNotConnectionField(typeName, fieldDefinitionContext.name().getText()))
                .filter(fieldDefinitionContext -> !fieldDefinitionContext.name().getText().endsWith(AGGREGATE_SUFFIX))
                .flatMap(fieldDefinitionContext -> {
                            if (manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
                                if (level <= layers) {
                                    return Stream.of(buildSelectionSet(fieldDefinitionContext.type(), level + 1, layers));
                                } else {
                                    return Stream.empty();
                                }
                            } else {
                                return Stream.of(fieldDefinitionContext.name().getText());
                            }
                        }
                )
                .collect(Collectors.joining(" "));

        stringBuilder.append(fieldNames);
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    private String getRpcName(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        String name = fieldDefinitionContext.name().getText();
        if (name.startsWith(INTROSPECTION_PREFIX)) {
            return "intro".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name.replaceFirst(INTROSPECTION_PREFIX, "")));
        }
        return name;
    }

    private String getRpcQueryRequestClassName(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        String name = fieldDefinitionContext.name().getText();
        if (name.startsWith(INTROSPECTION_PREFIX)) {
            return "QueryIntro".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name.replaceFirst(INTROSPECTION_PREFIX, ""))).concat("Request");
        }
        return "Query".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name.replaceFirst(INTROSPECTION_PREFIX, ""))).concat("Request");
    }

    private String getRpcMutationRequestClassName(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        String name = fieldDefinitionContext.name().getText();
        if (name.startsWith(INTROSPECTION_PREFIX)) {
            return "MutationIntro".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name.replaceFirst(INTROSPECTION_PREFIX, ""))).concat("Request");
        }
        return "Mutation".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name.replaceFirst(INTROSPECTION_PREFIX, ""))).concat("Request");
    }

    private String getRpcQueryResponseClassName(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        String name = fieldDefinitionContext.name().getText();
        if (name.startsWith(INTROSPECTION_PREFIX)) {
            return "QueryIntro".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name.replaceFirst(INTROSPECTION_PREFIX, ""))).concat("Response");
        }
        return "Query".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name.replaceFirst(INTROSPECTION_PREFIX, ""))).concat("Response");
    }

    private String getRpcMutationResponseClassName(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        String name = fieldDefinitionContext.name().getText();
        if (name.startsWith(INTROSPECTION_PREFIX)) {
            return "MutationIntro".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name.replaceFirst(INTROSPECTION_PREFIX, ""))).concat("Response");
        }
        return "Mutation".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name.replaceFirst(INTROSPECTION_PREFIX, ""))).concat("Response");
    }

    private String getRpcInputObjectName(GraphqlParser.TypeContext typeContext) {
        String name = manager.getFieldTypeName(typeContext);
        if (name.startsWith(INTROSPECTION_PREFIX)) {
            return "Intro".concat(name.replaceFirst(INTROSPECTION_PREFIX, ""));
        }
        return name;
    }

    private String getRpcInputObjectLowerCamelName(GraphqlParser.TypeContext typeContext) {
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, getRpcInputObjectName(typeContext));
    }

    private String getRpcEnumValueSuffixName(GraphqlParser.TypeContext typeContext) {
        return "_".concat(CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, getRpcInputObjectName(typeContext)));
    }

    private String getRpcInputValueName(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        String name = inputValueDefinitionContext.name().getText();
        if (name.startsWith(INTROSPECTION_PREFIX)) {
            return "intro".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name.replaceFirst(INTROSPECTION_PREFIX, "")));
        }
        return name;
    }

    private String getRpcGetInputValueName(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return "get".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, getRpcInputValueName(inputValueDefinitionContext)));
    }

    private String getRpcHasInputValueName(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return "has".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, getRpcInputValueName(inputValueDefinitionContext)));
    }

    private String getRpcGetInputValueListName(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return "get".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, getRpcInputValueName(inputValueDefinitionContext))).concat("List");
    }

    private String getRpcGetInputValueCountName(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return "get".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, getRpcInputValueName(inputValueDefinitionContext))).concat("Count");
    }

}
