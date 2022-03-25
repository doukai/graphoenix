package io.graphoenix.java.generator.builder;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.error.GraphQLProblem;
import io.graphoenix.java.generator.implementer.TypeManager;
import io.graphoenix.spi.annotation.TypeExpression;
import io.graphoenix.spi.annotation.TypeExpressions;
import io.graphoenix.spi.annotation.TypeInput;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import jakarta.annotation.Generated;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.Enum;
import org.eclipse.microprofile.graphql.Id;
import org.eclipse.microprofile.graphql.Input;
import org.eclipse.microprofile.graphql.Interface;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.NonNull;
import org.eclipse.microprofile.graphql.Type;
import org.tinylog.Logger;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.Modifier;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static io.graphoenix.core.error.GraphQLErrorType.UNSUPPORTED_FIELD_TYPE;

@ApplicationScoped
public class TypeSpecBuilder {

    private final IGraphQLDocumentManager manager;
    private final TypeManager typeManager;
    private GraphQLConfig graphQLConfig;

    @Inject
    public TypeSpecBuilder(IGraphQLDocumentManager manager, TypeManager typeManager, GraphQLConfig graphQLConfig) {
        this.manager = manager;
        this.typeManager = typeManager;
        this.graphQLConfig = graphQLConfig;
    }

    public TypeSpecBuilder setConfiguration(GraphQLConfig graphQLConfig) {
        this.graphQLConfig = graphQLConfig;
        return this;
    }

    private AnnotationSpec getGeneratedAnnotationSpec() {
        return AnnotationSpec.builder(Generated.class)
                .addMember("value", "$S", getClass().getName())
                .build();
    }

    public TypeSpec buildClass(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        TypeSpec.Builder builder = TypeSpec.classBuilder(objectTypeDefinitionContext.name().getText())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Type.class)
                .addAnnotation(getGeneratedAnnotationSpec());
        objectTypeDefinitionContext.fieldsDefinition().fieldDefinition()
                .forEach(fieldDefinitionContext -> {
                            FieldSpec fieldSpec = buildField(fieldDefinitionContext);
                            builder.addField(fieldSpec);
                            addGetterAndSetter(fieldSpec, builder, objectTypeDefinitionContext.implementsInterfaces());
                        }
                );
        if (objectTypeDefinitionContext.implementsInterfaces() != null) {
            objectTypeDefinitionContext.implementsInterfaces().typeName()
                    .forEach(typeNameContext -> {
                                builder.addSuperinterface(ClassName.get(graphQLConfig.getInterfaceTypePackageName(), typeNameContext.name().getText()));
                            }
                    );
        }
        if (objectTypeDefinitionContext.description() != null) {
            builder.addJavadoc("$S", objectTypeDefinitionContext.description().getText());
            builder.addAnnotation(
                    AnnotationSpec.builder(Description.class)
                            .addMember("value", "$S", objectTypeDefinitionContext.description().getText())
                            .build()
            );
        }
        Logger.info("class {} build success", objectTypeDefinitionContext.name().getText());
        return builder.build();
    }

    public TypeSpec buildClass(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext) {
        TypeSpec.Builder builder = TypeSpec.classBuilder(inputObjectTypeDefinitionContext.name().getText())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Input.class)
                .addAnnotation(getGeneratedAnnotationSpec());
        inputObjectTypeDefinitionContext.inputObjectValueDefinitions().inputValueDefinition()
                .forEach(inputValueDefinitionContext -> {
                            FieldSpec fieldSpec = buildField(inputValueDefinitionContext);
                            builder.addField(fieldSpec);
                            addGetterAndSetter(fieldSpec, builder, null);
                        }
                );
        if (inputObjectTypeDefinitionContext.description() != null) {
            builder.addJavadoc("$S", inputObjectTypeDefinitionContext.description().getText());
            builder.addAnnotation(
                    AnnotationSpec.builder(Description.class)
                            .addMember("value", "$S", inputObjectTypeDefinitionContext.description().getText())
                            .build()
            );
        }
        Logger.info("input class {} build success", inputObjectTypeDefinitionContext.name().getText());
        return builder.build();
    }

    public TypeSpec buildEnum(GraphqlParser.EnumTypeDefinitionContext enumTypeDefinitionContext) {
        TypeSpec.Builder builder = TypeSpec.enumBuilder(enumTypeDefinitionContext.name().getText())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Enum.class)
                .addAnnotation(getGeneratedAnnotationSpec());
        enumTypeDefinitionContext.enumValueDefinitions().enumValueDefinition()
                .forEach(enumValueDefinitionContext -> builder.addEnumConstant(enumValueDefinitionContext.enumValue().enumValueName().getText()));
        if (enumTypeDefinitionContext.description() != null) {
            builder.addJavadoc("$S", enumTypeDefinitionContext.description().getText());
            builder.addAnnotation(
                    AnnotationSpec.builder(Description.class)
                            .addMember("value", "$S", enumTypeDefinitionContext.description().getText())
                            .build()
            );
        }
        Logger.info("enum {} build success", enumTypeDefinitionContext.name().getText());
        return builder.build();
    }

    public TypeSpec buildInterface(GraphqlParser.InterfaceTypeDefinitionContext interfaceTypeDefinitionContext) {
        TypeSpec.Builder builder = TypeSpec.interfaceBuilder(interfaceTypeDefinitionContext.name().getText())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Interface.class)
                .addAnnotation(getGeneratedAnnotationSpec());
        interfaceTypeDefinitionContext.fieldsDefinition().fieldDefinition()
                .forEach(fieldDefinitionContext -> {
                            FieldSpec fieldSpec = buildInterfaceField(fieldDefinitionContext);
                            builder.addField(fieldSpec);
                            addInterfaceGetterAndSetter(fieldSpec, builder);
                        }
                );
        if (interfaceTypeDefinitionContext.implementsInterfaces() != null) {
            interfaceTypeDefinitionContext.implementsInterfaces().typeName()
                    .forEach(typeNameContext -> builder.addSuperinterface(ClassName.get(graphQLConfig.getInterfaceTypePackageName(), typeNameContext.name().getText())));
        }
        if (interfaceTypeDefinitionContext.description() != null) {
            builder.addJavadoc("$S", interfaceTypeDefinitionContext.description().getText());
            builder.addAnnotation(
                    AnnotationSpec.builder(Description.class)
                            .addMember("value", "$S", interfaceTypeDefinitionContext.description().getText())
                            .build()
            );
        }
        Logger.info("interface {} build success", interfaceTypeDefinitionContext.name().getText());
        return builder.build();
    }

    public TypeSpec buildAnnotation(GraphqlParser.DirectiveDefinitionContext directiveDefinitionContext) {
        TypeSpec.Builder builder = TypeSpec.annotationBuilder(directiveDefinitionContext.name().getText())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(getGeneratedAnnotationSpec());
        if (directiveDefinitionContext.argumentsDefinition() != null) {
            directiveDefinitionContext.argumentsDefinition().inputValueDefinition()
                    .forEach(inputValueDefinitionContext -> builder.addMethod(buildAnnotationMethod(inputValueDefinitionContext)));
        }
        builder.addAnnotation(AnnotationSpec.builder(Documented.class).build());
        builder.addAnnotation(
                AnnotationSpec.builder(Retention.class)
                        .addMember("value", "$T.$L", RetentionPolicy.class, RetentionPolicy.SOURCE)
                        .build()
        );
        CodeBlock.Builder codeBuilder = CodeBlock.builder();
        codeBuilder.add("{");
        codeBuilder.add(CodeBlock.join(buildElementTypeList(directiveDefinitionContext.directiveLocations()).stream().map(elementType -> CodeBlock.of("$T.$L", ElementType.class, elementType)).collect(Collectors.toList()), ","));
        codeBuilder.add("}");
        builder.addAnnotation(
                AnnotationSpec.builder(Target.class)
                        .addMember("value", codeBuilder.build())
                        .build()
        );
        if (directiveDefinitionContext.description() != null) {
            builder.addJavadoc("$S", directiveDefinitionContext.description().getText());
            builder.addAnnotation(
                    AnnotationSpec.builder(Description.class)
                            .addMember("value", "$S", directiveDefinitionContext.description().getText())
                            .build()
            );
        }
        Logger.info("directive annotation {} build success", directiveDefinitionContext.name().getText());
        return builder.build();
    }

    public TypeSpec buildAnnotation(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext) {
        TypeSpec.Builder builder = TypeSpec.annotationBuilder(inputObjectTypeDefinitionContext.name().getText())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(getGeneratedAnnotationSpec());
        inputObjectTypeDefinitionContext.inputObjectValueDefinitions().inputValueDefinition()
                .stream().filter(inputValueDefinitionContext -> !manager.isInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type())))
                .forEach(inputValueDefinitionContext -> builder.addMethod(buildAnnotationMethod(inputValueDefinitionContext)));
        builder.addAnnotation(AnnotationSpec.builder(Documented.class).build());
        builder.addAnnotation(
                AnnotationSpec.builder(Retention.class)
                        .addMember("value", "$T.$L", RetentionPolicy.class, RetentionPolicy.SOURCE)
                        .build()
        );
        builder.addAnnotation(
                AnnotationSpec.builder(Target.class)
                        .addMember("value", "$T.$L", ElementType.class, ElementType.ANNOTATION_TYPE)
                        .build()
        );
        if (inputObjectTypeDefinitionContext.description() != null) {
            builder.addJavadoc("$S", inputObjectTypeDefinitionContext.description().getText());
            builder.addAnnotation(
                    AnnotationSpec.builder(Description.class)
                            .addMember("value", "$S", inputObjectTypeDefinitionContext.description().getText())
                            .build()
            );
        }
        Logger.info("input annotation {} build success", inputObjectTypeDefinitionContext.name().getText());
        return builder.build();
    }

    public List<ElementType> buildElementTypeList(GraphqlParser.DirectiveLocationsContext directiveLocationsContext) {

        List<ElementType> elementTypeList = new ArrayList<>();
        if (directiveLocationsContext.directiveLocation() != null) {
            elementTypeList.add(buildElementType(directiveLocationsContext.directiveLocation()));
        } else if (directiveLocationsContext.directiveLocations() != null) {
            elementTypeList.addAll(buildElementTypeList(directiveLocationsContext.directiveLocations()));
        }
        return elementTypeList;
    }

    public ElementType buildElementType(GraphqlParser.DirectiveLocationContext directiveLocationContext) {
        switch (directiveLocationContext.name().getText()) {
            case "QUERY":
            case "MUTATION":
            case "SUBSCRIPTION":
                return ElementType.METHOD;
            case "FIELD":
            case "FIELD_DEFINITION":
            case "ENUM_VALUE":
            case "INPUT_FIELD_DEFINITION":
                return ElementType.FIELD;
            case "SCHEMA":
            case "OBJECT":
            case "INTERFACE":
            case "ENUM":
            case "UNION":
            case "INPUT_OBJECT":
            case "FRAGMENT_DEFINITION":
                return ElementType.TYPE;
            case "SCALAR":
            case "FRAGMENT_SPREAD":
            case "INLINE_FRAGMENT":
                return ElementType.TYPE_USE;
            case "ARGUMENT_DEFINITION":
                return ElementType.PARAMETER;

        }
        return null;
    }

    public FieldSpec buildField(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        boolean isKeyword = SourceVersion.isKeyword(fieldDefinitionContext.name().getText());
        FieldSpec.Builder builder = FieldSpec.builder(buildType(fieldDefinitionContext.type()), isKeyword ? "_".concat(fieldDefinitionContext.name().getText()) : fieldDefinitionContext.name().getText(), Modifier.PRIVATE);
        if (isKeyword) {
            builder.addAnnotation(AnnotationSpec.builder(Name.class).addMember("value", "$S", fieldDefinitionContext.name().getText()).build());
        }
        if (manager.getFieldTypeName(fieldDefinitionContext.type()).equals("ID")) {
            builder.addAnnotation(Id.class);
        }
        if (fieldDefinitionContext.type().nonNullType() != null) {
            builder.addAnnotation(NonNull.class);
        }
        if (fieldDefinitionContext.description() != null) {
            builder.addJavadoc("$S", fieldDefinitionContext.description().getText());
            builder.addAnnotation(
                    AnnotationSpec.builder(Description.class)
                            .addMember("value", "$S", fieldDefinitionContext.description().getText())
                            .build()
            );
        }
        Logger.info("class field {}.{} build success", manager.getFieldTypeName(fieldDefinitionContext.type()), fieldDefinitionContext.name().getText());
        return builder.build();
    }

    public FieldSpec buildInterfaceField(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        boolean isKeyword = SourceVersion.isKeyword(fieldDefinitionContext.name().getText());
        FieldSpec.Builder builder = FieldSpec.builder(buildType(fieldDefinitionContext.type()), isKeyword ? "_".concat(fieldDefinitionContext.name().getText()) : fieldDefinitionContext.name().getText(), Modifier.STATIC, Modifier.FINAL, Modifier.PUBLIC);
        if (isKeyword) {
            builder.addAnnotation(AnnotationSpec.builder(Name.class).addMember("value", "$S", fieldDefinitionContext.name().getText()).build());
        }
        builder.initializer("$L", "null");
        if (fieldDefinitionContext.type().nonNullType() != null) {
            builder.addAnnotation(NonNull.class);
        }
        if (fieldDefinitionContext.description() != null) {
            builder.addJavadoc("$S", fieldDefinitionContext.description().getText());
            builder.addAnnotation(
                    AnnotationSpec.builder(Description.class)
                            .addMember("value", "$S", fieldDefinitionContext.description().getText())
                            .build()
            );
        }
        Logger.info("interface field {}.{} build success", manager.getFieldTypeName(fieldDefinitionContext.type()), fieldDefinitionContext.name().getText());
        return builder.build();
    }

    public FieldSpec buildField(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        boolean isKeyword = SourceVersion.isKeyword(inputValueDefinitionContext.name().getText());
        FieldSpec.Builder builder = FieldSpec.builder(buildType(inputValueDefinitionContext.type()), isKeyword ? "_".concat(inputValueDefinitionContext.name().getText()) : inputValueDefinitionContext.name().getText(), Modifier.PRIVATE);
        if (inputValueDefinitionContext.defaultValue() != null) {
            builder.addAnnotation(
                    AnnotationSpec.builder(DefaultValue.class)
                            .addMember("value", "$S", inputValueDefinitionContext.defaultValue().value().getText())
                            .build()
            );
        }
        if (isKeyword) {
            builder.addAnnotation(AnnotationSpec.builder(Name.class).addMember("value", "$S", inputValueDefinitionContext.name().getText()).build());
        }
        if (inputValueDefinitionContext.type().nonNullType() != null) {
            builder.addAnnotation(NonNull.class);
        }
        if (inputValueDefinitionContext.description() != null) {
            builder.addJavadoc("$S", inputValueDefinitionContext.description().getText());
            builder.addAnnotation(
                    AnnotationSpec.builder(Description.class)
                            .addMember("value", "$S", inputValueDefinitionContext.description().getText())
                            .build()
            );
        }
        Logger.info("input class field {}.{} build success", manager.getFieldTypeName(inputValueDefinitionContext.type()), inputValueDefinitionContext.name().getText());
        return builder.build();
    }

    public MethodSpec buildAnnotationMethod(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        boolean isKeyword = SourceVersion.isKeyword(inputValueDefinitionContext.name().getText());
        MethodSpec.Builder builder = MethodSpec.methodBuilder(isKeyword ? "_".concat(inputValueDefinitionContext.name().getText()) : inputValueDefinitionContext.name().getText())
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(buildType(inputValueDefinitionContext.type(), true));
        if (isKeyword) {
            builder.addAnnotation(AnnotationSpec.builder(Name.class).addMember("value", "$S", inputValueDefinitionContext.name().getText()).build());
        }
        if (inputValueDefinitionContext.defaultValue() != null) {
            builder.defaultValue(buildDefaultValue(inputValueDefinitionContext, inputValueDefinitionContext.defaultValue().value()));
        }
        if (inputValueDefinitionContext.description() != null) {
            builder.addJavadoc("$S", inputValueDefinitionContext.description().getText());
            builder.addAnnotation(
                    AnnotationSpec.builder(Description.class)
                            .addMember("value", "$S", inputValueDefinitionContext.description().getText())
                            .build()
            );
        }
        Logger.info("input annotation field {}.{} build success", manager.getFieldTypeName(inputValueDefinitionContext.type()), inputValueDefinitionContext.name().getText());
        return builder.build();
    }

    private CodeBlock buildDefaultValue(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ValueContext valueContext) {
        if (valueContext.StringValue() != null) {
            return CodeBlock.of("$L", valueContext.StringValue().getText());
        } else if (valueContext.IntValue() != null) {
            return CodeBlock.of("$L", valueContext.IntValue().getText());
        } else if (valueContext.FloatValue() != null) {
            return CodeBlock.of("$L", valueContext.FloatValue().getText());
        } else if (valueContext.BooleanValue() != null) {
            return CodeBlock.of("$L", valueContext.BooleanValue().getText());
        } else if (valueContext.enumValue() != null) {
            return CodeBlock.of("$T.$L", ClassName.get(graphQLConfig.getEnumTypePackageName(), manager.getFieldTypeName(inputValueDefinitionContext.type())), valueContext.enumValue().getText());
        } else if (valueContext.arrayValue() != null) {
            CodeBlock.Builder codeBuilder = CodeBlock.builder();
            codeBuilder.add("{");
            codeBuilder.add(CodeBlock.join(valueContext.arrayValue().value().stream().map(subValue -> buildDefaultValue(inputValueDefinitionContext, subValue)).collect(Collectors.toList()), ","));
            codeBuilder.add("}");
            return codeBuilder.build();
        }
        throw new GraphQLProblem(UNSUPPORTED_FIELD_TYPE.bind(valueContext.getText()));
    }

    public TypeName buildType(GraphqlParser.TypeContext typeContext) {
        return buildType(typeContext, false, 0);
    }

    public TypeName buildType(GraphqlParser.TypeContext typeContext, boolean isAnnotation) {
        return buildType(typeContext, isAnnotation, 0);
    }

    public TypeName buildType(GraphqlParser.TypeContext typeContext, boolean isAnnotation, int layer) {
        if (typeContext.typeName() != null) {
            return buildType(typeContext.typeName().name(), isAnnotation, layer);
        } else if (typeContext.listType() != null) {
            return buildType(typeContext.listType(), isAnnotation, layer);
        } else if (typeContext.nonNullType() != null) {
            if (typeContext.nonNullType().typeName() != null) {
                return buildType(typeContext.nonNullType().typeName().name(), isAnnotation, layer);
            } else if (typeContext.nonNullType().listType() != null) {
                return buildType(typeContext.nonNullType().listType(), isAnnotation, layer);
            }
        }
        throw new GraphQLProblem(UNSUPPORTED_FIELD_TYPE.bind(typeContext.getText()));
    }

    public TypeName buildExpressionType(GraphqlParser.TypeContext typeContext, boolean isAnnotation) {
        if (typeContext.typeName() != null) {
            return buildExpressionType(typeContext.typeName().name(), isAnnotation);
        } else if (typeContext.listType() != null) {
            return buildExpressionType(typeContext.listType().type(), isAnnotation);
        } else if (typeContext.nonNullType() != null) {
            if (typeContext.nonNullType().typeName() != null) {
                return buildExpressionType(typeContext.nonNullType().typeName().name(), isAnnotation);
            } else if (typeContext.nonNullType().listType() != null) {
                return buildExpressionType(typeContext.nonNullType().listType().type(), isAnnotation);
            }
        }
        throw new GraphQLProblem(UNSUPPORTED_FIELD_TYPE.bind(typeContext.getText()));
    }

    public TypeName buildType(GraphqlParser.NameContext nameContext, boolean isAnnotation, int layer) {
        if (manager.isScalar(nameContext.getText())) {
            Optional<GraphqlParser.ScalarTypeDefinitionContext> scaLar = manager.getScaLar(nameContext.getText());
            if (scaLar.isPresent()) {
                if (isAnnotation) {
                    return buildAnnotationType(scaLar.get());
                } else {
                    return buildType(scaLar.get());
                }
            }
        } else if (manager.isObject(nameContext.getText())) {
            Optional<GraphqlParser.ObjectTypeDefinitionContext> object = manager.getObject(nameContext.getText());
            if (object.isPresent()) {
                if (isAnnotation) {
                    return ClassName.get(graphQLConfig.getAnnotationPackageName(), object.get().name().getText() + layer);
                } else {
                    return ClassName.get(graphQLConfig.getObjectTypePackageName(), object.get().name().getText());
                }
            }
        } else if (manager.isEnum(nameContext.getText())) {
            Optional<GraphqlParser.EnumTypeDefinitionContext> enumType = manager.getEnum(nameContext.getText());
            if (enumType.isPresent()) {
                return ClassName.get(graphQLConfig.getEnumTypePackageName(), enumType.get().name().getText());
            }
        } else if (manager.isInterface(nameContext.getText())) {
            Optional<GraphqlParser.InterfaceTypeDefinitionContext> interfaceType = manager.getInterface(nameContext.getText());
            if (interfaceType.isPresent()) {
                return ClassName.get(graphQLConfig.getInterfaceTypePackageName(), interfaceType.get().name().getText());
            }
        } else if (manager.isInputObject(nameContext.getText())) {
            Optional<GraphqlParser.InputObjectTypeDefinitionContext> inputObject = manager.getInputObject(nameContext.getText());
            if (inputObject.isPresent()) {
                if (isAnnotation) {
                    return ClassName.get(graphQLConfig.getDirectivePackageName(), inputObject.get().name().getText());
                } else {
                    return ClassName.get(graphQLConfig.getInputObjectTypePackageName(), inputObject.get().name().getText());
                }
            }
        }
        throw new GraphQLProblem(UNSUPPORTED_FIELD_TYPE.bind(nameContext.getText()));
    }

    public TypeName buildExpressionType(GraphqlParser.NameContext nameContext, boolean isAnnotation) {
        if (manager.isScalar(nameContext.getText())) {
            Optional<GraphqlParser.ScalarTypeDefinitionContext> scaLar = manager.getScaLar(nameContext.getText());
            if (scaLar.isPresent()) {
                if (isAnnotation) {
                    return buildAnnotationType(scaLar.get());
                } else {
                    return buildType(scaLar.get());
                }
            }
        } else if (manager.isEnum(nameContext.getText())) {
            Optional<GraphqlParser.EnumTypeDefinitionContext> enumType = manager.getEnum(nameContext.getText());
            if (enumType.isPresent()) {
                return ClassName.get(graphQLConfig.getEnumTypePackageName(), enumType.get().name().getText());
            }
        }
        throw new GraphQLProblem(UNSUPPORTED_FIELD_TYPE.bind(nameContext.getText()));
    }

    public TypeName buildType(GraphqlParser.ListTypeContext listTypeContext, boolean isAnnotation, int layer) {
        if (isAnnotation) {
            return ArrayTypeName.of(buildType(listTypeContext.type(), true, layer));
        } else {
            return ParameterizedTypeName.get(ClassName.get(Collection.class), buildType(listTypeContext.type()));
        }
    }

    public TypeName buildType(GraphqlParser.ScalarTypeDefinitionContext scalarTypeDefinitionContext) {
        String name = scalarTypeDefinitionContext.name().getText();
        switch (name) {
            case "Int":
                return TypeName.get(Integer.class);
            case "Float":
                return TypeName.get(Float.class);
            case "ID":
            case "String":
                return TypeName.get(String.class);
            case "Boolean":
                return TypeName.get(Boolean.class);
        }
        throw new GraphQLProblem(UNSUPPORTED_FIELD_TYPE.bind(scalarTypeDefinitionContext.getText()));
    }

    public TypeName buildAnnotationType(GraphqlParser.ScalarTypeDefinitionContext scalarTypeDefinitionContext) {
        String name = scalarTypeDefinitionContext.name().getText();
        switch (name) {
            case "Int":
                return TypeName.get(int.class);
            case "Float":
                return TypeName.get(float.class);
            case "ID":
            case "String":
                return TypeName.get(String.class);
            case "Boolean":
                return TypeName.get(boolean.class);
        }
        throw new GraphQLProblem(UNSUPPORTED_FIELD_TYPE.bind(scalarTypeDefinitionContext.getText()));
    }

    public CodeBlock buildAnnotationDefaultValue(GraphqlParser.TypeContext typeContext, int layer) {
        if (manager.fieldTypeIsList(typeContext)) {
            return CodeBlock.of("$L", "{}");
        }
        if (manager.isScalar(manager.getFieldTypeName(typeContext))) {
            Optional<GraphqlParser.ScalarTypeDefinitionContext> scaLar = manager.getScaLar(manager.getFieldTypeName(typeContext));
            if (scaLar.isPresent()) {
                return buildAnnotationDefaultValue(scaLar.get());
            }
        } else if (manager.isEnum(manager.getFieldTypeName(typeContext))) {
            Optional<GraphqlParser.EnumTypeDefinitionContext> enumType = manager.getEnum(manager.getFieldTypeName(typeContext));
            if (enumType.isPresent()) {
                return CodeBlock.of(
                        "$T.$L",
                        ClassName.get(graphQLConfig.getEnumTypePackageName(), enumType.get().name().getText()),
                        enumType.get().enumValueDefinitions().enumValueDefinition(0).enumValue().enumValueName().getText()
                );
            }
        } else if (manager.isObject(manager.getFieldTypeName(typeContext))) {
            Optional<GraphqlParser.ObjectTypeDefinitionContext> object = manager.getObject(manager.getFieldTypeName(typeContext));
            if (object.isPresent()) {
                return CodeBlock.of(
                        "@$T",
                        ClassName.get(graphQLConfig.getAnnotationPackageName(), object.get().name().getText() + "Input" + layer)
                );
            }
        }
        throw new GraphQLProblem(UNSUPPORTED_FIELD_TYPE.bind(typeContext.getText()));
    }

    public CodeBlock buildAnnotationDefaultValue(GraphqlParser.ScalarTypeDefinitionContext scalarTypeDefinitionContext) {
        String name = scalarTypeDefinitionContext.name().getText();
        switch (name) {
            case "Int":
            case "Float":
                return CodeBlock.of("$L", 0);
            case "ID":
            case "String":
                return CodeBlock.of("$S", "");
            case "Boolean":
                return CodeBlock.of("$L", false);
        }
        throw new GraphQLProblem(UNSUPPORTED_FIELD_TYPE.bind(scalarTypeDefinitionContext.getText()));
    }

    public void addGetterAndSetter(FieldSpec fieldSpec, TypeSpec.Builder classBuilder, GraphqlParser.ImplementsInterfacesContext implementsInterfacesContext) {
        addGetter(fieldSpec, classBuilder, implementsInterfacesContext);
        addSetter(fieldSpec, classBuilder, implementsInterfacesContext);
    }

    private void addSetter(FieldSpec fieldSpec, TypeSpec.Builder classBuilder, GraphqlParser.ImplementsInterfacesContext implementsInterfacesContext) {
        String setterName = typeManager.getFieldSetterMethodName(fieldSpec.name);
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(setterName).addModifiers(Modifier.PUBLIC);
        methodBuilder.addParameter(fieldSpec.type, fieldSpec.name);
        methodBuilder.addStatement("this." + fieldSpec.name + " = " + fieldSpec.name);

        if (implementsInterfacesContext != null) {
            implementsInterfacesContext.typeName().forEach(typeNameContext -> {
                Optional<GraphqlParser.InterfaceTypeDefinitionContext> interfaceType = manager.getInterface(typeNameContext.name().getText());
                interfaceType.ifPresent(interfaceTypeDefinitionContext -> interfaceTypeDefinitionContext.fieldsDefinition().fieldDefinition().forEach(fieldDefinitionContext -> {
                    if (fieldSpec.name.equals(fieldDefinitionContext.name().getText())) {
                        methodBuilder.addAnnotation(Override.class);
                    }
                }));
            });
        }
        classBuilder.addMethod(methodBuilder.build());
    }

    public void addGetter(FieldSpec fieldSpec, TypeSpec.Builder classBuilder, GraphqlParser.ImplementsInterfacesContext implementsInterfacesContext) {
        String getterName = typeManager.getFieldGetterMethodName(fieldSpec.name);
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(getterName).returns(fieldSpec.type).addModifiers(Modifier.PUBLIC);
        methodBuilder.addStatement("return this." + fieldSpec.name);

        if (implementsInterfacesContext != null) {
            implementsInterfacesContext.typeName().forEach(typeNameContext -> {
                Optional<GraphqlParser.InterfaceTypeDefinitionContext> interfaceType = manager.getInterface(typeNameContext.name().getText());
                interfaceType.ifPresent(interfaceTypeDefinitionContext -> interfaceTypeDefinitionContext.fieldsDefinition().fieldDefinition().forEach(fieldDefinitionContext -> {
                    if (fieldSpec.name.equals(fieldDefinitionContext.name().getText())) {
                        methodBuilder.addAnnotation(Override.class);
                    }
                }));
            });
        }

        classBuilder.addMethod(methodBuilder.build());
    }

    public void addInterfaceGetterAndSetter(FieldSpec fieldSpec, TypeSpec.Builder classBuilder) {
        addInterfaceGetter(fieldSpec, classBuilder);
        addInterfaceSetter(fieldSpec, classBuilder);
    }

    private void addInterfaceSetter(FieldSpec fieldSpec, TypeSpec.Builder classBuilder) {
        String setterName = typeManager.getFieldSetterMethodName(fieldSpec.name);
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(setterName).addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC);
        methodBuilder.addParameter(fieldSpec.type, fieldSpec.name);
        classBuilder.addMethod(methodBuilder.build());
    }

    public void addInterfaceGetter(FieldSpec fieldSpec, TypeSpec.Builder classBuilder) {
        String getterName = typeManager.getFieldGetterMethodName(fieldSpec.name);
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(getterName).returns(fieldSpec.type).addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC);
        classBuilder.addMethod(methodBuilder.build());
    }

    public Stream<TypeSpec> buildObjectTypeExpressionAnnotations() {
        return IntStream.range(0, graphQLConfig.getInputLayers())
                .mapToObj(this::buildObjectTypeExpressionAnnotations)
                .flatMap(typeSpecStream -> typeSpecStream);
    }

    public Stream<TypeSpec> buildObjectTypeExpressionAnnotations(int layer) {
        return manager.getObjects()
                .filter(objectTypeDefinitionContext ->
                        !manager.isQueryOperationType(objectTypeDefinitionContext.name().getText()) &&
                                !manager.isMutationOperationType(objectTypeDefinitionContext.name().getText()) &&
                                !manager.isSubscriptionOperationType(objectTypeDefinitionContext.name().getText())
                )
                .map(objectTypeDefinitionContext -> ObjectTypeToInputExpressionAnnotation(objectTypeDefinitionContext, layer));
    }

    public TypeSpec ObjectTypeToInputExpressionAnnotation(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext, int layer) {

        TypeSpec.Builder builder = TypeSpec.annotationBuilder(objectTypeDefinitionContext.name().getText() + "Expression" + layer)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(
                        AnnotationSpec.builder(Retention.class)
                                .addMember("value", "$T.$L", RetentionPolicy.class, RetentionPolicy.SOURCE)
                                .build()
                )
                .addAnnotation(
                        AnnotationSpec.builder(Target.class)
                                .addMember("value", "$T.$L", ElementType.class, ElementType.METHOD)
                                .build()
                )
                .addAnnotation(TypeExpression.class)
                .addMethod(
                        MethodSpec.methodBuilder("opr")
                                .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                                .returns(ClassName.get(graphQLConfig.getEnumTypePackageName(), "Operator"))
                                .defaultValue("$T.$L", ClassName.get(graphQLConfig.getEnumTypePackageName(), "Operator"), "EQ")
                                .build()
                )
                .addMethods(
                        manager.getFields(objectTypeDefinitionContext.name().getText())
                                .filter(fieldDefinitionContext ->
                                        manager.isScalar(manager.getFieldTypeName(fieldDefinitionContext.type())) ||
                                                manager.isEnum(manager.getFieldTypeName(fieldDefinitionContext.type()))
                                )
                                .filter(fieldDefinitionContext -> manager.isNotFunctionField(objectTypeDefinitionContext.name().getText(), fieldDefinitionContext.name().getText()))
                                .map(fieldDefinitionContext ->
                                        MethodSpec.methodBuilder(fieldDefinitionContext.name().getText())
                                                .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                                                .returns(ArrayTypeName.of(buildExpressionType(fieldDefinitionContext.type(), true)))
                                                .defaultValue(CodeBlock.of("$L", "{}"))
                                                .build()
                                )
                                .collect(Collectors.toList())
                )
                .addMethods(
                        manager.getFields(objectTypeDefinitionContext.name().getText())
                                .filter(fieldDefinitionContext ->
                                        manager.isScalar(manager.getFieldTypeName(fieldDefinitionContext.type())) ||
                                                manager.isEnum(manager.getFieldTypeName(fieldDefinitionContext.type()))
                                )
                                .filter(fieldDefinitionContext -> manager.isNotFunctionField(objectTypeDefinitionContext.name().getText(), fieldDefinitionContext.name().getText()))
                                .map(fieldDefinitionContext ->
                                        MethodSpec.methodBuilder("$".concat(fieldDefinitionContext.name().getText()))
                                                .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                                                .returns(ArrayTypeName.of(String.class))
                                                .defaultValue(CodeBlock.of("$L", "{}"))
                                                .build()
                                )
                                .collect(Collectors.toList())
                );

        if (layer < graphQLConfig.getInputLayers() - 1) {
            builder.addMethods(
                    manager.getFields(objectTypeDefinitionContext.name().getText())
                            .filter(fieldDefinitionContext ->
                                    manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type()))
                            )
                            .map(fieldDefinitionContext ->
                                    MethodSpec.methodBuilder(fieldDefinitionContext.name().getText())
                                            .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                                            .returns(ArrayTypeName.of(ClassName.get(graphQLConfig.getAnnotationPackageName(), manager.getFieldTypeName(fieldDefinitionContext.type()) + "Expressions" + (layer + 1))))
                                            .defaultValue(CodeBlock.of("$L", "{}"))
                                            .build()
                            )
                            .collect(Collectors.toList())
            );
        }
        Logger.info("annotation {}Expression{} build success", objectTypeDefinitionContext.name().getText(), layer);
        return builder.build();
    }

    public Stream<TypeSpec> buildObjectTypeExpressionsAnnotations() {
        return IntStream.range(0, graphQLConfig.getInputLayers())
                .mapToObj(this::buildObjectTypeExpressionsAnnotations)
                .flatMap(typeSpecStream -> typeSpecStream);
    }

    public Stream<TypeSpec> buildObjectTypeExpressionsAnnotations(int layer) {
        return manager.getObjects()
                .filter(objectTypeDefinitionContext ->
                        !manager.isQueryOperationType(objectTypeDefinitionContext.name().getText()) &&
                                !manager.isMutationOperationType(objectTypeDefinitionContext.name().getText()) &&
                                !manager.isSubscriptionOperationType(objectTypeDefinitionContext.name().getText())
                )
                .map(objectTypeDefinitionContext -> {
                            TypeSpec.Builder builder = TypeSpec.annotationBuilder(objectTypeDefinitionContext.name().getText() + "Expressions" + layer)
                                    .addModifiers(Modifier.PUBLIC)
                                    .addAnnotation(
                                            AnnotationSpec.builder(Retention.class)
                                                    .addMember("value", "$T.$L", RetentionPolicy.class, RetentionPolicy.SOURCE)
                                                    .build()
                                    )
                                    .addAnnotation(
                                            AnnotationSpec.builder(Target.class)
                                                    .addMember("value", "$T.$L", ElementType.class, ElementType.METHOD)
                                                    .build()
                                    )
                                    .addAnnotation(TypeExpressions.class)
                                    .addMethod(
                                            MethodSpec.methodBuilder("cond")
                                                    .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                                                    .returns(ClassName.get(graphQLConfig.getEnumTypePackageName(), "Conditional"))
                                                    .defaultValue("$T.$L", ClassName.get(graphQLConfig.getEnumTypePackageName(), "Conditional"), "AND")
                                                    .build()
                                    )
                                    .addMethod(
                                            MethodSpec.methodBuilder("value")
                                                    .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                                                    .returns(ArrayTypeName.of(ClassName.get("", objectTypeDefinitionContext.name().getText() + "Expression" + layer)))
                                                    .defaultValue("$L", "{}")
                                                    .build()
                                    );
                            Logger.info("annotation {}Expressions{} build success", objectTypeDefinitionContext.name().getText(), layer);
                            return builder.build();
                        }
                );
    }

    public Stream<TypeSpec> buildObjectTypeInputAnnotations() {
        return IntStream.range(0, graphQLConfig.getInputLayers())
                .mapToObj(this::buildObjectTypeInputAnnotations)
                .flatMap(typeSpecStream -> typeSpecStream);
    }

    public Stream<TypeSpec> buildObjectTypeInputAnnotations(int layer) {
        return manager.getObjects()
                .filter(objectTypeDefinitionContext ->
                        !manager.isQueryOperationType(objectTypeDefinitionContext.name().getText()) &&
                                !manager.isMutationOperationType(objectTypeDefinitionContext.name().getText()) &&
                                !manager.isSubscriptionOperationType(objectTypeDefinitionContext.name().getText())
                )
                .map(objectTypeDefinitionContext -> {
                            TypeSpec.Builder builder = TypeSpec.annotationBuilder(objectTypeDefinitionContext.name().getText() + "Input" + layer)
                                    .addModifiers(Modifier.PUBLIC)
                                    .addAnnotation(
                                            AnnotationSpec.builder(Retention.class)
                                                    .addMember("value", "$T.$L", RetentionPolicy.class, RetentionPolicy.SOURCE)
                                                    .build()
                                    )
                                    .addAnnotation(
                                            AnnotationSpec.builder(Target.class)
                                                    .addMember("value", "$T.$L", ElementType.class, ElementType.METHOD)
                                                    .build()
                                    )
                                    .addAnnotation(TypeInput.class)
                                    .addMethods(
                                            manager.getFields(objectTypeDefinitionContext.name().getText())
                                                    .filter(fieldDefinitionContext ->
                                                            manager.isScalar(manager.getFieldTypeName(fieldDefinitionContext.type())) ||
                                                                    manager.isEnum(manager.getFieldTypeName(fieldDefinitionContext.type()))
                                                    )
                                                    .filter(fieldDefinitionContext -> manager.isNotFunctionField(objectTypeDefinitionContext.name().getText(), fieldDefinitionContext.name().getText()))
                                                    .map(fieldDefinitionContext ->
                                                            MethodSpec.methodBuilder(fieldDefinitionContext.name().getText())
                                                                    .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                                                                    .returns(buildType(fieldDefinitionContext.type(), true, layer))
                                                                    .defaultValue(buildAnnotationDefaultValue(fieldDefinitionContext.type(), layer))
                                                                    .build()
                                                    )
                                                    .collect(Collectors.toList())
                                    )
                                    .addMethods(
                                            manager.getFields(objectTypeDefinitionContext.name().getText())
                                                    .filter(fieldDefinitionContext -> manager.isNotFunctionField(objectTypeDefinitionContext.name().getText(), fieldDefinitionContext.name().getText()))
                                                    .map(fieldDefinitionContext ->
                                                            MethodSpec.methodBuilder("$".concat(fieldDefinitionContext.name().getText()))
                                                                    .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                                                                    .returns(String.class)
                                                                    .defaultValue("$S", "")
                                                                    .build()
                                                    )
                                                    .collect(Collectors.toList())
                                    );
                            if (layer < graphQLConfig.getInputLayers() - 1) {
                                builder.addMethods(
                                        manager.getFields(objectTypeDefinitionContext.name().getText())
                                                .filter(fieldDefinitionContext ->
                                                        manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type()))
                                                )
                                                .map(fieldDefinitionContext ->
                                                        MethodSpec.methodBuilder(fieldDefinitionContext.name().getText())
                                                                .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                                                                .returns(
                                                                        manager.fieldTypeIsList(fieldDefinitionContext.type()) ?
                                                                                ArrayTypeName.of(ClassName.get(graphQLConfig.getAnnotationPackageName(), manager.getFieldTypeName(fieldDefinitionContext.type()) + "Input" + (layer + 1))) :
                                                                                ClassName.get(graphQLConfig.getAnnotationPackageName(), manager.getFieldTypeName(fieldDefinitionContext.type()) + "Input" + (layer + 1))
                                                                )
                                                                .defaultValue(buildAnnotationDefaultValue(fieldDefinitionContext.type(), layer + 1))
                                                                .build()
                                                )
                                                .collect(Collectors.toList())
                                );
                            }
                            Logger.info("annotation {}Input{} build success", objectTypeDefinitionContext.name().getText(), layer);
                            return builder.build();
                        }
                );
    }
}
