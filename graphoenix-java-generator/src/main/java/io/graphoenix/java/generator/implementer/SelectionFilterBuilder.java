package io.graphoenix.java.generator.implementer;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.handler.GraphQLFieldFormatter;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;
import jakarta.json.spi.JsonProvider;
import org.tinylog.Logger;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.graphoenix.core.utils.TypeNameUtil.TYPE_NAME_UTIL;

@ApplicationScoped
public class SelectionFilterBuilder {

    private final IGraphQLDocumentManager manager;
    private final TypeManager typeManager;
    private final GraphQLConfig graphQLConfig;

    @Inject
    public SelectionFilterBuilder(IGraphQLDocumentManager manager, TypeManager typeManager, GraphQLConfig graphQLConfig) {
        this.manager = manager;
        this.typeManager = typeManager;
        this.graphQLConfig = graphQLConfig;
    }

    public void writeToFiler(Filer filer) throws IOException {
        this.buildClass().writeTo(filer);
        Logger.info("SelectionFilter build success");
    }

    private JavaFile buildClass() {
        TypeSpec typeSpec = buildSelectionFilter();
        return JavaFile.builder(graphQLConfig.getHandlerPackageName(), typeSpec).build();
    }

    private TypeSpec buildSelectionFilter() {
        return TypeSpec.classBuilder("SelectionFilter")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(ApplicationScoped.class)
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
                                ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(GraphQLFieldFormatter.class)),
                                "formatter",
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
                .addMethod(buildConstructor())
                .addMethods(buildTypeMethods())
                .addMethods(buildListTypeMethods())
                .build();
    }

    private MethodSpec buildConstructor() {
        return MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Inject.class)
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(IGraphQLDocumentManager.class)), "manager")
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(GraphQLFieldFormatter.class)), "formatter")
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(JsonProvider.class)), "jsonProvider")
                .addStatement("this.manager = manager")
                .addStatement("this.formatter = formatter")
                .addStatement("this.jsonProvider = jsonProvider")
                .build();
    }

    private List<MethodSpec> buildTypeMethods() {
        return manager.getObjects()
                .filter(objectTypeDefinitionContext -> !manager.isQueryOperationType(objectTypeDefinitionContext.name().getText()))
                .filter(objectTypeDefinitionContext -> !manager.isMutationOperationType(objectTypeDefinitionContext.name().getText()))
                .filter(objectTypeDefinitionContext -> !manager.isSubscriptionOperationType(objectTypeDefinitionContext.name().getText()))
                .map(this::buildTypeMethod)
                .collect(Collectors.toList());
    }

    private MethodSpec buildTypeMethod(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        String typeParameterName = typeManager.typeToLowerCamelName(objectTypeDefinitionContext.name().getText());
        ClassName typeClassName;
        Optional<String> className = typeManager.getClassName(objectTypeDefinitionContext);
        if (className.isPresent()) {
            typeClassName = TYPE_NAME_UTIL.bestGuess(className.get());
        } else {
            typeClassName = ClassName.get(graphQLConfig.getObjectTypePackageName(), objectTypeDefinitionContext.name().getText());
        }
        MethodSpec.Builder builder = MethodSpec.methodBuilder(typeParameterName)
                .addModifiers(Modifier.PUBLIC)
                .returns(ClassName.get(JsonValue.class))
                .addParameter(typeClassName, typeParameterName)
                .addParameter(ClassName.get(GraphqlParser.SelectionSetContext.class), "selectionSet");

        builder.beginControlFlow("if (selectionSet != null && $L != null)", typeParameterName)
                .addStatement("$T objectBuilder = jsonProvider.get().createObjectBuilder()", ClassName.get(JsonObjectBuilder.class))
                .beginControlFlow("for ($T selectionContext : selectionSet.selection().stream().flatMap(selectionContext -> manager.get().fragmentUnzip($S, selectionContext)).collect($T.toList()))",
                        ClassName.get(GraphqlParser.SelectionContext.class),
                        objectTypeDefinitionContext.name().getText(),
                        ClassName.get(Collectors.class)
                )
                .addStatement("String selectionName = $T.ofNullable(selectionContext.field().alias()).map(aliasContext -> aliasContext.name().getText()).orElse(selectionContext.field().name().getText())", ClassName.get(Optional.class));

        int index = 0;
        List<GraphqlParser.FieldDefinitionContext> fieldDefinitionContextList = objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                .filter(manager::isNotFetchField)
                .collect(Collectors.toList());
        for (GraphqlParser.FieldDefinitionContext fieldDefinitionContext : fieldDefinitionContextList) {
            String fieldGetterMethodName = typeManager.getFieldGetterMethodName(fieldDefinitionContext);
            String fieldParameterName = typeManager.typeToLowerCamelName(fieldDefinitionContext.type());
            if (index == 0) {
                builder.beginControlFlow("if (selectionContext.field().name().getText().equals($S))", fieldDefinitionContext.name().getText());
            } else {
                builder.nextControlFlow("else if (selectionContext.field().name().getText().equals($S))", fieldDefinitionContext.name().getText());
            }
            if (manager.fieldTypeIsList(fieldDefinitionContext.type())) {
                builder.addStatement("$T arrayBuilder = jsonProvider.get().createArrayBuilder()", ClassName.get(JsonArrayBuilder.class))
                        .beginControlFlow("if ($L.$L() != null)",
                                typeParameterName,
                                fieldGetterMethodName
                        );
                if (manager.isScalar(manager.getFieldTypeName(fieldDefinitionContext.type())) || manager.isEnum(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
                    Optional<GraphqlParser.DirectiveContext> format = typeManager.getFormat(fieldDefinitionContext);
                    Optional<String> value = format.flatMap(typeManager::getFormatValue);
                    Optional<String> locale = format.flatMap(typeManager::getFormatLocale);
                    if (value.isPresent() && locale.isPresent()) {
                        builder.addStatement("$L.$L().forEach(item -> arrayBuilder.add(formatter.get().format($S, $S, item)))",
                                typeParameterName,
                                fieldGetterMethodName,
                                value.get(),
                                locale.get()
                        );
                    } else if (value.isPresent()) {
                        builder.addStatement("$L.$L().forEach(item -> arrayBuilder.add(formatter.get().format($S, null, item)))",
                                typeParameterName,
                                fieldGetterMethodName,
                                value.get()
                        );
                    } else {
                        builder.addStatement("$L.$L().forEach(item -> arrayBuilder.add(formatter.get().format(null, null, item)))",
                                typeParameterName,
                                fieldGetterMethodName
                        );
                    }
                } else if (manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
                    builder.addStatement("$L.$L().forEach(item -> arrayBuilder.add($L(item, selectionContext.field().selectionSet())))",
                            typeParameterName,
                            fieldGetterMethodName,
                            fieldParameterName
                    );
                }
                builder.addStatement("objectBuilder.add(selectionName, arrayBuilder)")
                        .nextControlFlow("else")
                        .addStatement("objectBuilder.add(selectionName, $T.NULL)", ClassName.get(JsonValue.class))
                        .endControlFlow();
            } else {
                if (manager.isScalar(manager.getFieldTypeName(fieldDefinitionContext.type())) || manager.isEnum(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
                    Optional<GraphqlParser.DirectiveContext> format = typeManager.getFormat(fieldDefinitionContext);
                    Optional<String> value = format.flatMap(typeManager::getFormatValue);
                    Optional<String> locale = format.flatMap(typeManager::getFormatLocale);
                    if (value.isPresent() && locale.isPresent()) {
                        builder.addStatement("objectBuilder.add(selectionName, formatter.get().format($S, $S, $L.$L()))",
                                value.get(),
                                locale.get(),
                                typeParameterName,
                                fieldGetterMethodName
                        );
                    } else if (value.isPresent()) {
                        builder.addStatement("objectBuilder.add(selectionName, formatter.get().format($S, null, $L.$L()))",
                                value.get(),
                                typeParameterName,
                                fieldGetterMethodName
                        );
                    } else {
                        builder.addStatement("objectBuilder.add(selectionName, formatter.get().format(null, null, $L.$L()))",
                                typeParameterName,
                                fieldGetterMethodName
                        );
                    }
                } else if (manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
                    builder.addStatement("objectBuilder.add(selectionName ,$L($L.$L(),selectionContext.field().selectionSet()))",
                            fieldParameterName,
                            typeParameterName,
                            fieldGetterMethodName
                    );
                }
            }
            if (index == fieldDefinitionContextList.size() - 1) {
                builder.endControlFlow();
            }
            index++;
        }
        builder.endControlFlow()
                .addStatement("return objectBuilder.build()")
                .endControlFlow()
                .addStatement("return $T.NULL", ClassName.get(JsonValue.class));
        return builder.build();
    }

    private List<MethodSpec> buildListTypeMethods() {
        return manager.getObjects()
                .filter(objectTypeDefinitionContext -> !manager.isQueryOperationType(objectTypeDefinitionContext.name().getText()))
                .filter(objectTypeDefinitionContext -> !manager.isMutationOperationType(objectTypeDefinitionContext.name().getText()))
                .filter(objectTypeDefinitionContext -> !manager.isSubscriptionOperationType(objectTypeDefinitionContext.name().getText()))
                .map(this::buildListTypeMethod)
                .collect(Collectors.toList());
    }

    private MethodSpec buildListTypeMethod(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        String typeParameterName = typeManager.typeToLowerCamelName(objectTypeDefinitionContext.name().getText());
        String listTypeParameterName = typeParameterName.concat("List");
        ClassName typeClassName;
        Optional<String> className = typeManager.getClassName(objectTypeDefinitionContext);
        if (className.isPresent()) {
            typeClassName = TYPE_NAME_UTIL.bestGuess(className.get());
        } else {
            typeClassName = ClassName.get(graphQLConfig.getObjectTypePackageName(), objectTypeDefinitionContext.name().getText());
        }
        MethodSpec.Builder builder = MethodSpec.methodBuilder(listTypeParameterName)
                .addModifiers(Modifier.PUBLIC)
                .returns(ClassName.get(JsonValue.class))
                .addParameter(ParameterizedTypeName.get(ClassName.get(Collection.class), typeClassName), listTypeParameterName)
                .addParameter(ClassName.get(GraphqlParser.SelectionSetContext.class), "selectionSet");

        builder.beginControlFlow("if (selectionSet != null && $L != null)", listTypeParameterName)
                .addStatement("$T arrayBuilder = jsonProvider.get().createArrayBuilder()", ClassName.get(JsonArrayBuilder.class))
                .addStatement("$L.forEach(item -> arrayBuilder.add($L(item, selectionSet)))",
                        listTypeParameterName,
                        typeParameterName
                )
                .addStatement("return arrayBuilder.build()")
                .endControlFlow()
                .addStatement("return $T.NULL", ClassName.get(JsonValue.class));
        return builder.build();
    }
}
