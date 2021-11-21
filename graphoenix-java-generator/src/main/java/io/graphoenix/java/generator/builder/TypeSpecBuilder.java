package io.graphoenix.java.generator.builder;

import com.google.common.base.CaseFormat;
import com.squareup.javapoet.*;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.spi.config.JavaGeneratorConfig;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;

import javax.lang.model.element.Modifier;
import javax.validation.constraints.NotNull;
import java.lang.annotation.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class TypeSpecBuilder {

    private final IGraphQLDocumentManager manager;
    private final JavaGeneratorConfig configuration;

    public TypeSpecBuilder(IGraphQLDocumentManager manager, JavaGeneratorConfig configuration) {
        this.manager = manager;
        this.configuration = configuration;
    }

    public TypeSpec buildClass(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        TypeSpec.Builder builder = TypeSpec.classBuilder(objectTypeDefinitionContext.name().getText())
                .addModifiers(Modifier.PUBLIC);
        objectTypeDefinitionContext.fieldsDefinition().fieldDefinition()
                .forEach(
                        fieldDefinitionContext -> {
                            FieldSpec fieldSpec = buildField(fieldDefinitionContext);
                            builder.addField(fieldSpec);
                            addGetterAndSetter(fieldSpec, builder, objectTypeDefinitionContext.implementsInterfaces());
                        }
                );
        if (objectTypeDefinitionContext.implementsInterfaces() != null) {
            objectTypeDefinitionContext.implementsInterfaces().typeName()
                    .forEach(
                            typeNameContext -> {
                                builder.addSuperinterface(ClassName.get(configuration.getInterfaceTypePackageName(), typeNameContext.name().getText()));
                            }
                    );
        }
        if (objectTypeDefinitionContext.description() != null) {
            builder.addJavadoc("$S", objectTypeDefinitionContext.description().getText());
        }
        return builder.build();
    }

    public TypeSpec buildClass(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext) {
        TypeSpec.Builder builder = TypeSpec.classBuilder(inputObjectTypeDefinitionContext.name().getText())
                .addModifiers(Modifier.PUBLIC);
        inputObjectTypeDefinitionContext.inputObjectValueDefinitions().inputValueDefinition()
                .forEach(
                        inputValueDefinitionContext -> {
                            FieldSpec fieldSpec = buildField(inputValueDefinitionContext);
                            builder.addField(fieldSpec);
                            addGetterAndSetter(fieldSpec, builder, null);
                        }
                );
        if (inputObjectTypeDefinitionContext.description() != null) {
            builder.addJavadoc("$S", inputObjectTypeDefinitionContext.description().getText());
        }
        return builder.build();
    }

    public TypeSpec buildEnum(GraphqlParser.EnumTypeDefinitionContext enumTypeDefinitionContext) {
        TypeSpec.Builder builder = TypeSpec.enumBuilder(enumTypeDefinitionContext.name().getText())
                .addModifiers(Modifier.PUBLIC);
        enumTypeDefinitionContext.enumValueDefinitions().enumValueDefinition()
                .forEach(enumValueDefinitionContext -> builder.addEnumConstant(enumValueDefinitionContext.enumValue().enumValueName().getText()));
        if (enumTypeDefinitionContext.description() != null) {
            builder.addJavadoc("$S", enumTypeDefinitionContext.description().getText());
        }
        return builder.build();
    }

    public TypeSpec buildInterface(GraphqlParser.InterfaceTypeDefinitionContext interfaceTypeDefinitionContext) {
        TypeSpec.Builder builder = TypeSpec.interfaceBuilder(interfaceTypeDefinitionContext.name().getText())
                .addModifiers(Modifier.PUBLIC);
        interfaceTypeDefinitionContext.fieldsDefinition().fieldDefinition()
                .forEach(
                        fieldDefinitionContext -> {
                            FieldSpec fieldSpec = buildInterfaceField(fieldDefinitionContext);
                            builder.addField(fieldSpec);
                            addInterfaceGetterAndSetter(fieldSpec, builder);
                        }
                );
        if (interfaceTypeDefinitionContext.implementsInterfaces() != null) {
            interfaceTypeDefinitionContext.implementsInterfaces().typeName()
                    .forEach(typeNameContext -> builder.addSuperinterface(ClassName.get(configuration.getInterfaceTypePackageName(), typeNameContext.name().getText())));
        }
        if (interfaceTypeDefinitionContext.description() != null) {
            builder.addJavadoc("$S", interfaceTypeDefinitionContext.description().getText());
        }
        return builder.build();
    }

    public TypeSpec buildAnnotation(GraphqlParser.DirectiveDefinitionContext directiveDefinitionContext) {
        TypeSpec.Builder builder = TypeSpec.annotationBuilder(directiveDefinitionContext.name().getText())
                .addModifiers(Modifier.PUBLIC);
        directiveDefinitionContext.argumentsDefinition().inputValueDefinition()
                .forEach(inputValueDefinitionContext -> builder.addMethod(buildAnnotationMethod(inputValueDefinitionContext)));
        builder.addAnnotation(AnnotationSpec.builder(Documented.class).build());
        builder.addAnnotation(
                AnnotationSpec.builder(Retention.class)
                        .addMember("value", "$T.$L", RetentionPolicy.class, RetentionPolicy.SOURCE)
                        .build()
        );
        List<ElementType> elementTypeList = buildElementTypeList(directiveDefinitionContext.directiveLocations());
        CodeBlock.Builder codeBuilder = CodeBlock.builder();
        codeBuilder.add("{");
        IntStream.range(0, elementTypeList.size())
                .forEach(index -> {
                    String format = index == elementTypeList.size() - 1 ? "$T.$L" : "$T.$L,";
                    codeBuilder.add(format, ElementType.class, elementTypeList.get(index));
                });
        codeBuilder.add("}");
        builder.addAnnotation(
                AnnotationSpec.builder(Target.class)
                        .addMember("value", codeBuilder.build())
                        .build()
        );
        if (directiveDefinitionContext.description() != null) {
            builder.addJavadoc("$S", directiveDefinitionContext.description().getText());
        }
        return builder.build();
    }

    public TypeSpec buildAnnotation(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext) {
        TypeSpec.Builder builder = TypeSpec.annotationBuilder(inputObjectTypeDefinitionContext.name().getText())
                .addModifiers(Modifier.PUBLIC);
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
        }
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
        FieldSpec.Builder builder = FieldSpec.builder(buildType(fieldDefinitionContext.type()), fieldDefinitionContext.name().getText(), Modifier.PRIVATE);
        if (fieldDefinitionContext.type().nonNullType() != null) {
            builder.addAnnotation(NotNull.class);
        }
        if (fieldDefinitionContext.description() != null) {
            builder.addJavadoc("$S", fieldDefinitionContext.description().getText());
        }
        return builder.build();
    }

    public FieldSpec buildInterfaceField(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        FieldSpec.Builder builder = FieldSpec.builder(buildType(fieldDefinitionContext.type()), fieldDefinitionContext.name().getText(), Modifier.STATIC, Modifier.FINAL, Modifier.PUBLIC);
        builder.initializer("$L", "null");
        if (fieldDefinitionContext.type().nonNullType() != null) {
            builder.addAnnotation(NotNull.class);
        }
        if (fieldDefinitionContext.description() != null) {
            builder.addJavadoc("$S", fieldDefinitionContext.description().getText());
        }
        return builder.build();
    }

    public FieldSpec buildField(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        FieldSpec.Builder builder = FieldSpec.builder(buildType(inputValueDefinitionContext.type()), inputValueDefinitionContext.name().getText(), Modifier.PRIVATE);
        if (inputValueDefinitionContext.type().nonNullType() != null) {
            builder.addAnnotation(NotNull.class);
        }
        if (inputValueDefinitionContext.description() != null) {
            builder.addJavadoc("$S", inputValueDefinitionContext.description().getText());
        }
        return builder.build();
    }

    public MethodSpec buildAnnotationMethod(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(inputValueDefinitionContext.name().getText())
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(buildType(inputValueDefinitionContext.type(), true));
        if (inputValueDefinitionContext.description() != null) {
            builder.addJavadoc("$S", inputValueDefinitionContext.description().getText());
        }
        return builder.build();
    }


    public TypeName buildType(GraphqlParser.TypeContext typeContext) {
        return buildType(typeContext, false);
    }

    public TypeName buildType(GraphqlParser.TypeContext typeContext, boolean isAnnotation) {
        if (typeContext.typeName() != null) {
            return buildType(typeContext.typeName().name(), isAnnotation);
        } else if (typeContext.listType() != null) {
            return buildType(typeContext.listType(), isAnnotation);
        } else if (typeContext.nonNullType() != null) {
            if (typeContext.nonNullType().typeName() != null) {
                return buildType(typeContext.nonNullType().typeName().name(), isAnnotation);
            } else if (typeContext.nonNullType().listType() != null) {
                return buildType(typeContext.nonNullType().listType(), isAnnotation);
            }
        }
        return null;
    }

    public TypeName buildType(GraphqlParser.NameContext nameContext, boolean isAnnotation) {
        if (manager.isScaLar(nameContext.getText())) {
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
                return ClassName.get(configuration.getObjectTypePackageName(), object.get().name().getText());
            }
        } else if (manager.isEnum(nameContext.getText())) {
            Optional<GraphqlParser.EnumTypeDefinitionContext> enumType = manager.getEnum(nameContext.getText());
            if (enumType.isPresent()) {
                return ClassName.get(configuration.getEnumTypePackageName(), enumType.get().name().getText());
            }
        } else if (manager.isInterface(nameContext.getText())) {
            Optional<GraphqlParser.InterfaceTypeDefinitionContext> interfaceType = manager.getInterface(nameContext.getText());
            if (interfaceType.isPresent()) {
                return ClassName.get(configuration.getInterfaceTypePackageName(), interfaceType.get().name().getText());
            }
        } else if (manager.isInputObject(nameContext.getText())) {
            Optional<GraphqlParser.InputObjectTypeDefinitionContext> inputObject = manager.getInputObject(nameContext.getText());
            if (inputObject.isPresent()) {
                if (isAnnotation) {
                    return ClassName.get(configuration.getDirectivePackageName(), inputObject.get().name().getText());
                } else {
                    return ClassName.get(configuration.getInputObjectTypePackageName(), inputObject.get().name().getText());
                }
            }
        }
        return null;
    }

    public TypeName buildType(GraphqlParser.ListTypeContext listTypeContext, boolean isAnnotation) {
        if (isAnnotation) {
            return ArrayTypeName.of(buildType(listTypeContext.type(), true));
        } else {
            return ParameterizedTypeName.get(ClassName.get(Set.class), buildType(listTypeContext.type()));
        }
    }

    public TypeName buildType(GraphqlParser.ScalarTypeDefinitionContext scalarTypeDefinitionContext) {
        String name = scalarTypeDefinitionContext.name().getText();
        switch (name) {
            case "ID":
            case "Int":
                return TypeName.get(Integer.class);
            case "Float":
                return TypeName.get(Float.class);
            case "String":
                return TypeName.get(String.class);
            case "Boolean":
                return TypeName.get(Boolean.class);
        }
        return null;
    }

    public TypeName buildAnnotationType(GraphqlParser.ScalarTypeDefinitionContext scalarTypeDefinitionContext) {
        String name = scalarTypeDefinitionContext.name().getText();
        switch (name) {
            case "ID":
            case "Int":
                return TypeName.get(int.class);
            case "Float":
                return TypeName.get(float.class);
            case "String":
                return TypeName.get(String.class);
            case "Boolean":
                return TypeName.get(boolean.class);
        }
        return null;
    }

    public void addGetterAndSetter(FieldSpec fieldSpec, TypeSpec.Builder classBuilder, GraphqlParser.ImplementsInterfacesContext implementsInterfacesContext) {
        addGetter(fieldSpec, classBuilder, implementsInterfacesContext);
        addSetter(fieldSpec, classBuilder, implementsInterfacesContext);
    }

    private void addSetter(FieldSpec fieldSpec, TypeSpec.Builder classBuilder, GraphqlParser.ImplementsInterfacesContext implementsInterfacesContext) {
        String setterName = "set" + capitalizeFirstLetter(fieldSpec.name);
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
        String getterName = "get" + capitalizeFirstLetter(fieldSpec.name);
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
        String setterName = "set" + capitalizeFirstLetter(fieldSpec.name);
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(setterName).addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC);
        methodBuilder.addParameter(fieldSpec.type, fieldSpec.name);
        classBuilder.addMethod(methodBuilder.build());
    }

    public void addInterfaceGetter(FieldSpec fieldSpec, TypeSpec.Builder classBuilder) {
        String getterName = "get" + capitalizeFirstLetter(fieldSpec.name);
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(getterName).returns(fieldSpec.type).addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC);
        classBuilder.addMethod(methodBuilder.build());
    }

    private String capitalizeFirstLetter(final String fieldName) {
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, fieldName);
    }

    public Stream<TypeSpec> buildObjectTypeExpressionAnnotations() {
        return manager.getObjects()
                .filter(objectTypeDefinitionContext ->
                        !manager.isQueryOperationType(objectTypeDefinitionContext.name().getText()) &&
                                !manager.isMutationOperationType(objectTypeDefinitionContext.name().getText()) &&
                                !manager.isSubscriptionOperationType(objectTypeDefinitionContext.name().getText())
                )
                .map(objectTypeDefinitionContext ->
                        TypeSpec.annotationBuilder(objectTypeDefinitionContext.name().getText() + "Expression")
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
                                .addMethod(
                                        MethodSpec.methodBuilder("opr")
                                                .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                                                .returns(ClassName.get(configuration.getEnumTypePackageName(), "Operator"))
                                                .defaultValue("$T.$L", ClassName.get(configuration.getEnumTypePackageName(), "Operator"), "EQ")
                                                .build()
                                )
                                .addMethods(
                                        manager.getFields(objectTypeDefinitionContext.name().getText())
                                                .filter(fieldDefinitionContext ->
                                                        manager.isScaLar(
                                                                manager.getFieldTypeName(fieldDefinitionContext.type())) ||
                                                                manager.isEnum(manager.getFieldTypeName(fieldDefinitionContext.type()))
                                                )
                                                .map(fieldDefinitionContext ->
                                                        MethodSpec.methodBuilder(fieldDefinitionContext.name().getText())
                                                                .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                                                                .returns(String.class)
                                                                .defaultValue("$S", "")
                                                                .build()
                                                )
                                                .collect(Collectors.toList())
                                )
                                .build()
                );
    }

    public Stream<TypeSpec> buildObjectTypeExpressionsAnnotations() {
        return manager.getObjects()
                .filter(objectTypeDefinitionContext ->
                        !manager.isQueryOperationType(objectTypeDefinitionContext.name().getText()) &&
                                !manager.isMutationOperationType(objectTypeDefinitionContext.name().getText()) &&
                                !manager.isSubscriptionOperationType(objectTypeDefinitionContext.name().getText())
                )
                .map(objectTypeDefinitionContext ->
                        TypeSpec.annotationBuilder(objectTypeDefinitionContext.name().getText() + "Expressions")
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
                                .addMethod(
                                        MethodSpec.methodBuilder("cond")
                                                .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                                                .returns(ClassName.get(configuration.getEnumTypePackageName(), "Conditional"))
                                                .defaultValue("$T.$L", ClassName.get(configuration.getEnumTypePackageName(), "Conditional"), "AND")
                                                .build()
                                )
                                .addMethod(
                                        MethodSpec.methodBuilder("value")
                                                .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                                                .returns(ArrayTypeName.of(ClassName.get("", objectTypeDefinitionContext.name().getText() + "Expression")))
                                                .defaultValue("$L", "{}")
                                                .build()
                                )
                                .addMethods(
                                        manager.getFields(objectTypeDefinitionContext.name().getText())
                                                .filter(fieldDefinitionContext -> manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type())))
                                                .map(fieldDefinitionContext ->
                                                        MethodSpec.methodBuilder(fieldDefinitionContext.name().getText())
                                                                .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                                                                .returns(ArrayTypeName.of(ClassName.get(configuration.getAnnotationPackageName(), manager.getFieldTypeName(fieldDefinitionContext.type()) + "Expression")))
                                                                .defaultValue("$L", "{}")
                                                                .build()
                                                )
                                                .collect(Collectors.toList())
                                )
                                .build()
                );
    }

    public Stream<TypeSpec> buildObjectTypeInputAnnotations() {
        return manager.getObjects()
                .filter(objectTypeDefinitionContext ->
                        !manager.isQueryOperationType(objectTypeDefinitionContext.name().getText()) &&
                                !manager.isMutationOperationType(objectTypeDefinitionContext.name().getText()) &&
                                !manager.isSubscriptionOperationType(objectTypeDefinitionContext.name().getText())
                )
                .map(objectTypeDefinitionContext ->
                        TypeSpec.annotationBuilder(objectTypeDefinitionContext.name().getText() + "Input")
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
                                .addMethods(
                                        manager.getFields(objectTypeDefinitionContext.name().getText())
                                                .filter(fieldDefinitionContext ->
                                                        manager.isScaLar(
                                                                manager.getFieldTypeName(fieldDefinitionContext.type())) ||
                                                                manager.isEnum(manager.getFieldTypeName(fieldDefinitionContext.type()))
                                                )
                                                .map(fieldDefinitionContext ->
                                                        MethodSpec.methodBuilder(fieldDefinitionContext.name().getText())
                                                                .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                                                                .returns(String.class)
                                                                .defaultValue("$S", "")
                                                                .build()
                                                )
                                                .collect(Collectors.toList())
                                )
                                .build()
                );
    }

    public Stream<TypeSpec> buildObjectTypeInputsAnnotations() {
        return manager.getObjects()
                .filter(objectTypeDefinitionContext ->
                        !manager.isQueryOperationType(objectTypeDefinitionContext.name().getText()) &&
                                !manager.isMutationOperationType(objectTypeDefinitionContext.name().getText()) &&
                                !manager.isSubscriptionOperationType(objectTypeDefinitionContext.name().getText())
                )
                .map(objectTypeDefinitionContext ->
                        TypeSpec.annotationBuilder(objectTypeDefinitionContext.name().getText() + "Inputs")
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
                                .addMethod(
                                        MethodSpec.methodBuilder("value")
                                                .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                                                .returns(ArrayTypeName.of(ClassName.get("", objectTypeDefinitionContext.name().getText() + "Input")))
                                                .defaultValue("$L", "{}")
                                                .build()
                                )
                                .addMethods(
                                        manager.getFields(objectTypeDefinitionContext.name().getText())
                                                .filter(fieldDefinitionContext -> manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type())))
                                                .map(fieldDefinitionContext ->
                                                        MethodSpec.methodBuilder(fieldDefinitionContext.name().getText())
                                                                .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                                                                .returns(ArrayTypeName.of(ClassName.get(configuration.getAnnotationPackageName(), manager.getFieldTypeName(fieldDefinitionContext.type()) + "Input")))
                                                                .defaultValue("$L", "{}")
                                                                .build()
                                                )
                                                .collect(Collectors.toList())
                                )
                                .build()
                );
    }
}
