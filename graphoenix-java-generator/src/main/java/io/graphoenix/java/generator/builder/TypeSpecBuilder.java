package io.graphoenix.java.generator.builder;

import com.dslplatform.json.CompiledJson;
import com.google.common.base.CaseFormat;
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
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.core.handler.PackageManager;
import io.graphoenix.java.generator.implementer.TypeManager;
import io.graphoenix.spi.annotation.Ignore;
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
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static io.graphoenix.core.error.GraphQLErrorType.UNSUPPORTED_FIELD_TYPE;
import static io.graphoenix.core.utils.TypeNameUtil.TYPE_NAME_UTIL;
import static io.graphoenix.java.generator.utils.TypeUtil.TYPE_UTIL;
import static io.graphoenix.spi.constant.Hammurabi.AFTER_INPUT_NAME;
import static io.graphoenix.spi.constant.Hammurabi.AGGREGATE_SUFFIX;
import static io.graphoenix.spi.constant.Hammurabi.BEFORE_INPUT_NAME;
import static io.graphoenix.spi.constant.Hammurabi.CURSOR_DIRECTIVE_NAME;
import static io.graphoenix.spi.constant.Hammurabi.EXPRESSION_SUFFIX;
import static io.graphoenix.spi.constant.Hammurabi.FIRST_INPUT_NAME;
import static io.graphoenix.spi.constant.Hammurabi.GROUP_BY_INPUT_NAME;
import static io.graphoenix.spi.constant.Hammurabi.INPUT_SUFFIX;
import static io.graphoenix.spi.constant.Hammurabi.LAST_INPUT_NAME;
import static io.graphoenix.spi.constant.Hammurabi.LIST_INPUT_NAME;
import static io.graphoenix.spi.constant.Hammurabi.OFFSET_INPUT_NAME;
import static io.graphoenix.spi.constant.Hammurabi.ORDER_BY_INPUT_NAME;
import static io.graphoenix.spi.constant.Hammurabi.ORDER_BY_SUFFIX;
import static io.graphoenix.spi.constant.Hammurabi.PAGE_INFO_NAME;

@ApplicationScoped
public class TypeSpecBuilder {

    private final IGraphQLDocumentManager manager;
    private final PackageManager packageManager;
    private final TypeManager typeManager;
    private final GraphQLConfig graphQLConfig;

    @Inject
    public TypeSpecBuilder(IGraphQLDocumentManager manager, PackageManager packageManager, TypeManager typeManager, GraphQLConfig graphQLConfig) {
        this.manager = manager;
        this.packageManager = packageManager;
        this.typeManager = typeManager;
        this.graphQLConfig = graphQLConfig;
    }

    private AnnotationSpec getGeneratedAnnotationSpec() {
        return AnnotationSpec.builder(Generated.class)
                .addMember("value", "$S", getClass().getName())
                .build();
    }

    private AnnotationSpec getSchemaBeanAnnotationSpec() {
        return AnnotationSpec.builder(Ignore.class)
                .build();
    }

    public TypeSpec buildClass(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        return buildClass(objectTypeDefinitionContext, objectTypeDefinitionContext.fieldsDefinition().fieldDefinition());
    }

    public TypeSpec buildClass(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext, List<GraphqlParser.FieldDefinitionContext> fieldDefinitionContextList) {
        TypeSpec.Builder builder = TypeSpec.classBuilder(objectTypeDefinitionContext.name().getText())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Type.class)
                .addAnnotation(CompiledJson.class)
                .addAnnotation(getGeneratedAnnotationSpec())
                .addAnnotation(getSchemaBeanAnnotationSpec());
        fieldDefinitionContextList
                .forEach(fieldDefinitionContext -> {
                            FieldSpec fieldSpec = buildField(fieldDefinitionContext);
                            builder.addField(fieldSpec);
                            addGetterAndSetter(fieldSpec, builder, objectTypeDefinitionContext.implementsInterfaces());
                        }
                );
        if (objectTypeDefinitionContext.implementsInterfaces() != null) {
            builder.addSuperinterfaces(
                    manager.getInterfaces(objectTypeDefinitionContext.implementsInterfaces())
                            .map(interfaceTypeDefinitionContext -> TYPE_NAME_UTIL.toClassName(packageManager.getClassName(interfaceTypeDefinitionContext)))
                            .collect(Collectors.toList())
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
                .addAnnotation(CompiledJson.class)
                .addAnnotation(getGeneratedAnnotationSpec())
                .addAnnotation(getSchemaBeanAnnotationSpec());
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
                .addAnnotation(getGeneratedAnnotationSpec())
                .addAnnotation(getSchemaBeanAnnotationSpec());
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
//                .addAnnotation(CompiledJson.class)
                .addAnnotation(getGeneratedAnnotationSpec())
                .addAnnotation(getSchemaBeanAnnotationSpec());
        interfaceTypeDefinitionContext.fieldsDefinition().fieldDefinition()
                .forEach(fieldDefinitionContext -> {
                            FieldSpec fieldSpec = buildInterfaceField(fieldDefinitionContext);
                            builder.addField(fieldSpec);
                            addInterfaceGetterAndSetter(fieldSpec, builder);
                        }
                );
        if (interfaceTypeDefinitionContext.implementsInterfaces() != null) {
            builder.addSuperinterfaces(
                    manager.getInterfaces(interfaceTypeDefinitionContext.implementsInterfaces())
                            .map(implementInterfaceTypeDefinitionContext -> TYPE_NAME_UTIL.toClassName(packageManager.getClassName(implementInterfaceTypeDefinitionContext)))
                            .collect(Collectors.toList())
            );
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
        TypeSpec.Builder builder = TypeSpec.annotationBuilder(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, directiveDefinitionContext.name().getText()))
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(getGeneratedAnnotationSpec())
                .addAnnotation(getSchemaBeanAnnotationSpec());
        if (directiveDefinitionContext.argumentsDefinition() != null) {
            directiveDefinitionContext.argumentsDefinition().inputValueDefinition()
                    .forEach(inputValueDefinitionContext -> builder.addMethod(buildAnnotationMethod(inputValueDefinitionContext)));
        }
        builder
                .addAnnotation(AnnotationSpec.builder(Documented.class).build())
                .addAnnotation(
                        AnnotationSpec.builder(Retention.class)
                                .addMember("value", "$T.$L", RetentionPolicy.class, RetentionPolicy.SOURCE)
                                .build()
                )
                .addAnnotation(
                        AnnotationSpec.builder(io.graphoenix.spi.annotation.Directive.class)
                                .addMember("value", "$S", directiveDefinitionContext.name().getText())
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
                .addAnnotation(getGeneratedAnnotationSpec())
                .addAnnotation(getSchemaBeanAnnotationSpec());
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

    public Set<ElementType> buildElementTypeList(GraphqlParser.DirectiveLocationsContext directiveLocationsContext) {
        return manager.getDirectiveLocations(directiveLocationsContext)
                .map(directiveLocationContext -> directiveLocationContext.name().getText())
                .map(this::buildElementType)
                .collect(Collectors.toSet());
    }

    public ElementType buildElementType(String locationName) {
        switch (locationName) {
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
            return CodeBlock.of("$T.$L",
                    TYPE_NAME_UTIL.toClassName(packageManager.getClassName(inputValueDefinitionContext.type())),
                    valueContext.enumValue().getText()
            );
        } else if (valueContext.arrayValue() != null) {
            CodeBlock.Builder codeBuilder = CodeBlock.builder();
            codeBuilder.add("{");
            codeBuilder.add(CodeBlock.join(valueContext.arrayValue().value().stream().map(subValue -> buildDefaultValue(inputValueDefinitionContext, subValue)).collect(Collectors.toList()), ","));
            codeBuilder.add("}");
            return codeBuilder.build();
        }
        throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE.bind(valueContext.getText()));
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
        throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE.bind(typeContext.getText()));
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
        throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE.bind(typeContext.getText()));
    }

    public TypeName buildType(GraphqlParser.NameContext nameContext, boolean isAnnotation, int layer) {
        if (manager.isScalar(nameContext.getText())) {
            Optional<GraphqlParser.ScalarTypeDefinitionContext> scalarType = manager.getScalar(nameContext.getText());
            if (scalarType.isPresent()) {
                if (isAnnotation) {
                    return buildAnnotationType(scalarType.get());
                } else {
                    return buildType(scalarType.get());
                }
            }
        } else if (manager.isObject(nameContext.getText())) {
            Optional<GraphqlParser.ObjectTypeDefinitionContext> object = manager.getObject(nameContext.getText());
            if (object.isPresent()) {
                return TYPE_NAME_UTIL.toClassName(packageManager.getClassName(object.get()));
            }
        } else if (manager.isEnum(nameContext.getText())) {
            Optional<GraphqlParser.EnumTypeDefinitionContext> enumType = manager.getEnum(nameContext.getText());
            if (enumType.isPresent()) {
                return TYPE_NAME_UTIL.toClassName(packageManager.getClassName(enumType.get()));
            }
        } else if (manager.isInterface(nameContext.getText())) {
            Optional<GraphqlParser.InterfaceTypeDefinitionContext> interfaceType = manager.getInterface(nameContext.getText());
            if (interfaceType.isPresent()) {
                return TYPE_NAME_UTIL.toClassName(packageManager.getClassName(interfaceType.get()));
            }
        } else if (manager.isInputObject(nameContext.getText())) {
            Optional<GraphqlParser.InputObjectTypeDefinitionContext> inputObject = manager.getInputObject(nameContext.getText());
            if (inputObject.isPresent()) {
                if (isAnnotation) {
                    return TYPE_NAME_UTIL.toClassName(packageManager.getAnnotationName(inputObject.get()));
                } else {
                    return TYPE_NAME_UTIL.toClassName(packageManager.getClassName(inputObject.get()));
                }
            }
        }
        throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE.bind(nameContext.getText()));
    }

    public TypeName buildExpressionType(GraphqlParser.NameContext nameContext, boolean isAnnotation) {
        if (manager.isScalar(nameContext.getText())) {
            Optional<GraphqlParser.ScalarTypeDefinitionContext> scalarType = manager.getScalar(nameContext.getText());
            if (scalarType.isPresent()) {
                if (isAnnotation) {
                    return buildAnnotationType(scalarType.get());
                } else {
                    return buildType(scalarType.get());
                }
            }
        } else if (manager.isEnum(nameContext.getText())) {
            Optional<GraphqlParser.EnumTypeDefinitionContext> enumType = manager.getEnum(nameContext.getText());
            if (enumType.isPresent()) {
                return TYPE_NAME_UTIL.toClassName(packageManager.getClassName(enumType.get()));
            }
        }
        throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE.bind(nameContext.getText()));
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
            case "ID":
            case "String":
                return TypeName.get(String.class);
            case "Boolean":
                return TypeName.get(Boolean.class);
            case "Int":
                return TypeName.get(Integer.class);
            case "Float":
                return TypeName.get(Float.class);
            case "BigInteger":
                return TypeName.get(BigInteger.class);
            case "BigDecimal":
                return TypeName.get(BigDecimal.class);
            case "Date":
                return TypeName.get(LocalDate.class);
            case "Time":
                return TypeName.get(LocalTime.class);
            case "DateTime":
            case "Timestamp":
                return TypeName.get(LocalDateTime.class);
        }
        throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE.bind(scalarTypeDefinitionContext.getText()));
    }

    public TypeName buildAnnotationType(GraphqlParser.ScalarTypeDefinitionContext scalarTypeDefinitionContext) {
        String name = scalarTypeDefinitionContext.name().getText();
        switch (name) {
            case "ID":
            case "String":
            case "Date":
            case "Time":
            case "DateTime":
            case "Timestamp":
                return TypeName.get(String.class);
            case "Boolean":
                return TypeName.get(boolean.class);
            case "Int":
            case "BigInteger":
                return TypeName.get(int.class);
            case "Float":
            case "BigDecimal":
                return TypeName.get(float.class);
        }
        throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE.bind(scalarTypeDefinitionContext.getText()));
    }

    public TypeName buildScalarOrEnumExpressionAnnotationType(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
        if (manager.isScalar(fieldTypeName)) {
            switch (fieldTypeName) {
                case "Boolean":
                    return ClassName.get(graphQLConfig.getAnnotationPackageName(), "Boolean".concat(EXPRESSION_SUFFIX));
                case "ID":
                    return ClassName.get(graphQLConfig.getAnnotationPackageName(), "ID".concat(EXPRESSION_SUFFIX));
                case "String":
                case "Date":
                case "Time":
                case "DateTime":
                case "Timestamp":
                    return ClassName.get(graphQLConfig.getAnnotationPackageName(), "String".concat(EXPRESSION_SUFFIX));
                case "Int":
                case "BigInteger":
                    return ClassName.get(graphQLConfig.getAnnotationPackageName(), "Int".concat(EXPRESSION_SUFFIX));
                case "Float":
                case "BigDecimal":
                    return ClassName.get(graphQLConfig.getAnnotationPackageName(), "Float".concat(EXPRESSION_SUFFIX));
            }
        } else if (manager.isEnum(fieldTypeName)) {
            Optional<GraphqlParser.InputObjectTypeDefinitionContext> enumExpression = manager.getInputObject(fieldTypeName.concat(EXPRESSION_SUFFIX));
            if (enumExpression.isPresent()) {
                return TYPE_NAME_UTIL.toClassName(packageManager.getAnnotationName(enumExpression.get()));
            }
        }
        throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE.bind(fieldTypeName));
    }

    public CodeBlock buildAnnotationDefaultValue(GraphqlParser.TypeContext typeContext) {
        return buildAnnotationDefaultValue(typeContext, 0);
    }

    public CodeBlock buildAnnotationDefaultValue(GraphqlParser.TypeContext typeContext, int layer) {
        if (manager.fieldTypeIsList(typeContext)) {
            return CodeBlock.of("$L", "{}");
        }
        if (manager.isScalar(manager.getFieldTypeName(typeContext))) {
            Optional<GraphqlParser.ScalarTypeDefinitionContext> scalarType = manager.getScalar(manager.getFieldTypeName(typeContext));
            if (scalarType.isPresent()) {
                return buildAnnotationDefaultValue(scalarType.get());
            }
        } else if (manager.isEnum(manager.getFieldTypeName(typeContext))) {
            Optional<GraphqlParser.EnumTypeDefinitionContext> enumType = manager.getEnum(manager.getFieldTypeName(typeContext));
            if (enumType.isPresent()) {
                return CodeBlock.of(
                        "$T.$L",
                        TYPE_NAME_UTIL.toClassName(packageManager.getClassName(enumType.get())),
                        enumType.get().enumValueDefinitions().enumValueDefinition(0).enumValue().enumValueName().getText()
                );
            }
        } else if (manager.isObject(manager.getFieldTypeName(typeContext))) {
            Optional<GraphqlParser.InputObjectTypeDefinitionContext> inputObject = manager.getInputObject(manager.getFieldTypeName(typeContext).concat(INPUT_SUFFIX));
            if (inputObject.isPresent()) {
                return CodeBlock.of(
                        "@$T",
                        TYPE_NAME_UTIL.toClassName(packageManager.getAnnotationName(inputObject.get()) + layer)
                );
            }
        }
        throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE.bind(typeContext.getText()));
    }

    public CodeBlock buildAnnotationDefaultValue(GraphqlParser.ScalarTypeDefinitionContext scalarTypeDefinitionContext) {
        String name = scalarTypeDefinitionContext.name().getText();
        switch (name) {
            case "ID":
            case "String":
            case "Date":
            case "Time":
            case "DateTime":
            case "Timestamp":
                return CodeBlock.of("$S", "");
            case "Boolean":
                return CodeBlock.of("$L", false);
            case "Int":
            case "Float":
            case "BigInteger":
            case "BigDecimal":
                return CodeBlock.of("$L", 0);
        }
        throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE.bind(scalarTypeDefinitionContext.getText()));
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
            manager.getInterfaces(implementsInterfacesContext)
                    .forEach(interfaceTypeDefinitionContext ->
                            interfaceTypeDefinitionContext.fieldsDefinition().fieldDefinition()
                                    .forEach(fieldDefinitionContext -> {
                                                if (fieldSpec.name.equals(fieldDefinitionContext.name().getText())) {
                                                    methodBuilder.addAnnotation(Override.class);
                                                }
                                            }
                                    )
                    );
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

    public Stream<TypeSpec> buildScalarTypeExpressionAnnotations() {
        return manager.getScalars()
                .filter(scalarTypeDefinitionContext -> manager.isInnerScalar(scalarTypeDefinitionContext.name().getText()))
                .map(this::scalarTypeToInputExpressionAnnotation);
    }

    public TypeSpec scalarTypeToInputExpressionAnnotation(GraphqlParser.ScalarTypeDefinitionContext scalarTypeDefinitionContext) {
        TypeSpec.Builder builder = TypeSpec.annotationBuilder(scalarTypeDefinitionContext.name().getText().concat(EXPRESSION_SUFFIX))
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
                                .returns(
                                        manager.getEnum("Operator")
                                                .map(packageManager::getClassName)
                                                .map(TYPE_NAME_UTIL::toClassName)
                                                .orElseGet(() -> ClassName.get(graphQLConfig.getEnumTypePackageName(), "Operator"))
                                )
                                .defaultValue("$T.$L",
                                        manager.getEnum("Operator")
                                                .map(packageManager::getClassName)
                                                .map(TYPE_NAME_UTIL::toClassName)
                                                .orElseGet(() -> ClassName.get(graphQLConfig.getEnumTypePackageName(), "Operator")),
                                        "EQ"
                                )
                                .build()
                )
                .addMethod(
                        MethodSpec.methodBuilder("val")
                                .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                                .returns(buildAnnotationType(scalarTypeDefinitionContext))
                                .defaultValue(buildAnnotationDefaultValue(scalarTypeDefinitionContext))
                                .build()
                )
                .addMethod(
                        MethodSpec.methodBuilder("in")
                                .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                                .returns(ArrayTypeName.of(buildAnnotationType(scalarTypeDefinitionContext)))
                                .defaultValue(CodeBlock.of("$L", "{}"))
                                .build()
                )
                .addMethod(
                        MethodSpec.methodBuilder("$val")
                                .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                                .returns(String.class)
                                .defaultValue(CodeBlock.of("$S", ""))
                                .build()
                )
                .addMethod(
                        MethodSpec.methodBuilder("$in")
                                .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                                .returns(String.class)
                                .defaultValue(CodeBlock.of("$S", ""))
                                .build()
                )
                .addMethod(
                        MethodSpec.methodBuilder("skipNull")
                                .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                                .returns(boolean.class)
                                .defaultValue(CodeBlock.of("$L", false))
                                .build()
                );

        Logger.info("annotation {}Expression build success", scalarTypeDefinitionContext.name().getText());
        return builder.build();
    }

    public Stream<TypeSpec> buildEnumTypeExpressionAnnotations() {
        return manager.getEnums()
                .filter(packageManager::isOwnPackage)
                .filter(manager::classNotExists)
                .map(this::enumTypeToInputExpressionAnnotation);
    }

    public TypeSpec enumTypeToInputExpressionAnnotation(GraphqlParser.EnumTypeDefinitionContext enumTypeDefinitionContext) {
        TypeSpec.Builder builder = TypeSpec.annotationBuilder(enumTypeDefinitionContext.name().getText() + EXPRESSION_SUFFIX)
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
                                .returns(
                                        manager.getEnum("Operator")
                                                .map(packageManager::getClassName)
                                                .map(TYPE_NAME_UTIL::toClassName)
                                                .orElseGet(() -> ClassName.get(graphQLConfig.getEnumTypePackageName(), "Operator"))
                                )
                                .defaultValue("$T.$L",
                                        manager.getEnum("Operator")
                                                .map(packageManager::getClassName)
                                                .map(TYPE_NAME_UTIL::toClassName)
                                                .orElseGet(() -> ClassName.get(graphQLConfig.getEnumTypePackageName(), "Operator")),
                                        "EQ"
                                )
                                .build()
                )
                .addMethod(
                        MethodSpec.methodBuilder("val")
                                .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                                .returns(
                                        manager.getEnum(enumTypeDefinitionContext.name().getText())
                                                .map(packageManager::getClassName)
                                                .map(TYPE_NAME_UTIL::toClassName)
                                                .orElseGet(() -> ClassName.get(graphQLConfig.getEnumTypePackageName(), enumTypeDefinitionContext.name().getText()))
                                )
                                .defaultValue(CodeBlock.of(
                                        "$T.$L",
                                        manager.getEnum(enumTypeDefinitionContext.name().getText())
                                                .map(packageManager::getClassName)
                                                .map(TYPE_NAME_UTIL::toClassName)
                                                .orElseGet(() -> ClassName.get(graphQLConfig.getEnumTypePackageName(), enumTypeDefinitionContext.name().getText())),
                                        enumTypeDefinitionContext.enumValueDefinitions().enumValueDefinition(0).enumValue().enumValueName().getText()
                                ))
                                .build()
                )
                .addMethod(
                        MethodSpec.methodBuilder("in")
                                .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                                .returns(
                                        ArrayTypeName.of(
                                                manager.getEnum(enumTypeDefinitionContext.name().getText())
                                                        .map(packageManager::getClassName)
                                                        .map(TYPE_NAME_UTIL::toClassName)
                                                        .orElseGet(() -> ClassName.get(graphQLConfig.getEnumTypePackageName(), enumTypeDefinitionContext.name().getText()))
                                        )
                                )
                                .defaultValue(CodeBlock.of("$L", "{}"))
                                .build()
                )
                .addMethod(
                        MethodSpec.methodBuilder("$val")
                                .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                                .returns(String.class)
                                .defaultValue(CodeBlock.of("$S", ""))
                                .build()
                )
                .addMethod(
                        MethodSpec.methodBuilder("$in")
                                .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                                .returns(String.class)
                                .defaultValue(CodeBlock.of("$S", ""))
                                .build()
                );
        Logger.info("annotation {}Expression build success", enumTypeDefinitionContext.name().getText());
        return builder.build();
    }

    public Stream<TypeSpec> buildObjectTypeExpressionAnnotations() {
        return IntStream.range(0, graphQLConfig.getInputLayers())
                .mapToObj(this::buildObjectTypeExpressionAnnotations)
                .flatMap(typeSpecStream -> typeSpecStream);
    }

    public Stream<TypeSpec> buildObjectTypeExpressionAnnotations(int layer) {
        return manager.getObjects()
                .filter(packageManager::isOwnPackage)
                .filter(manager::isNotOperationType)
                .filter(manager::classNotExists)
                .filter(manager::isNotContainerType)
                .filter(objectTypeDefinitionContext -> !objectTypeDefinitionContext.name().getText().equals(PAGE_INFO_NAME))
                .map(objectTypeDefinitionContext -> {
                            TypeSpec.Builder builder = TypeSpec.annotationBuilder(objectTypeDefinitionContext.name().getText().concat(EXPRESSION_SUFFIX) + layer)
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
                                                    .returns(
                                                            manager.getEnum("Conditional")
                                                                    .map(packageManager::getClassName)
                                                                    .map(TYPE_NAME_UTIL::toClassName)
                                                                    .orElseGet(() -> ClassName.get(graphQLConfig.getEnumTypePackageName(), "Conditional"))
                                                    )
                                                    .defaultValue("$T.$L",
                                                            manager.getEnum("Conditional")
                                                                    .map(packageManager::getClassName)
                                                                    .map(TYPE_NAME_UTIL::toClassName)
                                                                    .orElseGet(() -> ClassName.get(graphQLConfig.getEnumTypePackageName(), "Conditional")),
                                                            "AND"
                                                    )
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
                                                                    .returns(buildScalarOrEnumExpressionAnnotationType(fieldDefinitionContext))
                                                                    .defaultValue(CodeBlock.of("@$T", buildScalarOrEnumExpressionAnnotationType(fieldDefinitionContext)))
                                                                    .build()
                                                    )
                                                    .collect(Collectors.toList())
                                    )
                                    .addMethod(
                                            MethodSpec.methodBuilder(GROUP_BY_INPUT_NAME)
                                                    .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                                                    .returns(ArrayTypeName.of(String.class))
                                                    .defaultValue(CodeBlock.of("$L", "{}"))
                                                    .build()
                                    )
                                    .addMethod(
                                            MethodSpec.methodBuilder(ORDER_BY_INPUT_NAME)
                                                    .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                                                    .returns(ClassName.get(graphQLConfig.getAnnotationPackageName(), objectTypeDefinitionContext.name().getText().concat(ORDER_BY_SUFFIX) + layer))
                                                    .defaultValue(
                                                            CodeBlock.of(
                                                                    "@$T",
                                                                    ClassName.get(graphQLConfig.getAnnotationPackageName(), objectTypeDefinitionContext.name().getText().concat(ORDER_BY_SUFFIX) + layer)
                                                            )
                                                    )
                                                    .build()
                                    )
                                    .addMethod(
                                            MethodSpec.methodBuilder(FIRST_INPUT_NAME)
                                                    .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                                                    .returns(int.class)
                                                    .defaultValue(CodeBlock.of("$L", 0))
                                                    .build()
                                    )
                                    .addMethod(
                                            MethodSpec.methodBuilder("$".concat(FIRST_INPUT_NAME))
                                                    .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                                                    .returns(String.class)
                                                    .defaultValue(CodeBlock.of("$S", ""))
                                                    .build()
                                    )
                                    .addMethod(
                                            MethodSpec.methodBuilder(LAST_INPUT_NAME)
                                                    .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                                                    .returns(int.class)
                                                    .defaultValue(CodeBlock.of("$L", 0))
                                                    .build()
                                    )
                                    .addMethod(
                                            MethodSpec.methodBuilder("$".concat(LAST_INPUT_NAME))
                                                    .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                                                    .returns(String.class)
                                                    .defaultValue(CodeBlock.of("$S", ""))
                                                    .build()
                                    )
                                    .addMethod(
                                            MethodSpec.methodBuilder(OFFSET_INPUT_NAME)
                                                    .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                                                    .returns(int.class)
                                                    .defaultValue(CodeBlock.of("$L", 0))
                                                    .build()
                                    )
                                    .addMethod(
                                            MethodSpec.methodBuilder("$".concat(OFFSET_INPUT_NAME))
                                                    .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                                                    .returns(String.class)
                                                    .defaultValue(CodeBlock.of("$S", ""))
                                                    .build()
                                    );

                            manager.getFieldByDirective(objectTypeDefinitionContext.name().getText(), CURSOR_DIRECTIVE_NAME)
                                    .findFirst()
                                    .or(() -> manager.getObjectTypeIDFieldDefinition(objectTypeDefinitionContext.name().getText()))
                                    .ifPresent(cursorFieldDefinitionContext ->
                                            builder.addMethod(
                                                    MethodSpec.methodBuilder(AFTER_INPUT_NAME)
                                                            .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                                                            .returns(buildType(cursorFieldDefinitionContext.type(), true))
                                                            .defaultValue(buildAnnotationDefaultValue(cursorFieldDefinitionContext.type()))
                                                            .build()
                                            ).addMethod(
                                                    MethodSpec.methodBuilder(BEFORE_INPUT_NAME)
                                                            .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                                                            .returns(buildType(cursorFieldDefinitionContext.type(), true))
                                                            .defaultValue(buildAnnotationDefaultValue(cursorFieldDefinitionContext.type()))
                                                            .build()
                                            ).addMethod(
                                                    MethodSpec.methodBuilder("$".concat(AFTER_INPUT_NAME))
                                                            .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                                                            .returns(String.class)
                                                            .defaultValue(CodeBlock.of("$S", ""))
                                                            .build()
                                            ).addMethod(
                                                    MethodSpec.methodBuilder("$".concat(BEFORE_INPUT_NAME))
                                                            .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                                                            .returns(String.class)
                                                            .defaultValue(CodeBlock.of("$S", ""))
                                                            .build()
                                            )
                                    );

                            if (layer < graphQLConfig.getInputLayers() - 1) {
                                builder.addMethods(
                                        manager.getFields(objectTypeDefinitionContext.name().getText())
                                                .filter(fieldDefinitionContext -> manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type())))
                                                .filter(fieldDefinitionContext -> manager.isNotContainerType(fieldDefinitionContext.type()))
                                                .filter(fieldDefinitionContext -> manager.isNotConnectionField(objectTypeDefinitionContext.name().getText(), fieldDefinitionContext.name().getText()))
                                                .filter(fieldDefinitionContext -> !fieldDefinitionContext.name().getText().endsWith(AGGREGATE_SUFFIX))
                                                .map(fieldDefinitionContext ->
                                                        MethodSpec.methodBuilder(fieldDefinitionContext.name().getText())
                                                                .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                                                                .returns(ClassName.get(graphQLConfig.getAnnotationPackageName(), manager.getFieldTypeName(fieldDefinitionContext.type()).concat(EXPRESSION_SUFFIX) + (layer + 1)))
                                                                .defaultValue(
                                                                        CodeBlock.of(
                                                                                "@$T",
                                                                                ClassName.get(graphQLConfig.getAnnotationPackageName(), manager.getFieldTypeName(fieldDefinitionContext.type()).concat(EXPRESSION_SUFFIX) + (layer + 1))
                                                                        )
                                                                )
                                                                .build()
                                                )
                                                .collect(Collectors.toList())
                                ).addMethod(
                                        MethodSpec.methodBuilder("exs")
                                                .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                                                .returns(ArrayTypeName.of(ClassName.get(graphQLConfig.getAnnotationPackageName(), objectTypeDefinitionContext.name().getText().concat(EXPRESSION_SUFFIX) + (layer + 1))))
                                                .defaultValue(CodeBlock.of("$L", "{}"))
                                                .build()
                                );
                            }
                            Logger.info("annotation {}Expression{} build success", objectTypeDefinitionContext.name().getText(), layer);
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
                .filter(packageManager::isOwnPackage)
                .filter(manager::isNotOperationType)
                .filter(manager::classNotExists)
                .filter(manager::isNotContainerType)
                .filter(objectTypeDefinitionContext -> !objectTypeDefinitionContext.name().getText().equals(PAGE_INFO_NAME))
                .map(objectTypeDefinitionContext -> {
                            TypeSpec.Builder builder = TypeSpec.annotationBuilder(objectTypeDefinitionContext.name().getText().concat(INPUT_SUFFIX) + layer)
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
                                                .filter(fieldDefinitionContext -> manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type())))
                                                .filter(fieldDefinitionContext -> manager.isNotContainerType(fieldDefinitionContext.type()))
                                                .filter(fieldDefinitionContext -> manager.isNotConnectionField(objectTypeDefinitionContext.name().getText(), fieldDefinitionContext.name().getText()))
                                                .filter(fieldDefinitionContext -> !fieldDefinitionContext.name().getText().endsWith(AGGREGATE_SUFFIX))
                                                .map(fieldDefinitionContext ->
                                                        MethodSpec.methodBuilder(fieldDefinitionContext.name().getText())
                                                                .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                                                                .returns(
                                                                        manager.fieldTypeIsList(fieldDefinitionContext.type()) ?
                                                                                ArrayTypeName.of(ClassName.get(graphQLConfig.getAnnotationPackageName(), manager.getFieldTypeName(fieldDefinitionContext.type()).concat(INPUT_SUFFIX) + (layer + 1))) :
                                                                                ClassName.get(graphQLConfig.getAnnotationPackageName(), manager.getFieldTypeName(fieldDefinitionContext.type()).concat(INPUT_SUFFIX) + (layer + 1))
                                                                )
                                                                .defaultValue(buildAnnotationDefaultValue(fieldDefinitionContext.type(), layer + 1))
                                                                .build()
                                                )
                                                .collect(Collectors.toList())
                                )
                                        .addMethod(
                                                MethodSpec.methodBuilder(LIST_INPUT_NAME)
                                                        .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                                                        .returns(ArrayTypeName.of(ClassName.get(graphQLConfig.getAnnotationPackageName(), objectTypeDefinitionContext.name().getText().concat(INPUT_SUFFIX) + (layer + 1))))
                                                        .defaultValue(CodeBlock.of("$L", "{}"))
                                                        .build()
                                        )
                                        .addMethod(
                                                MethodSpec.methodBuilder("$".concat(LIST_INPUT_NAME))
                                                        .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                                                        .returns(String.class)
                                                        .defaultValue("$S", "")
                                                        .build()
                                        );
                            }
                            Logger.info("annotation {}Input{} build success", objectTypeDefinitionContext.name().getText(), layer);
                            return builder.build();
                        }
                );
    }

    public Stream<TypeSpec> buildObjectTypeOrderByAnnotations() {
        return IntStream.range(0, graphQLConfig.getInputLayers())
                .mapToObj(this::buildObjectTypeOrderByAnnotations)
                .flatMap(typeSpecStream -> typeSpecStream);
    }

    public Stream<TypeSpec> buildObjectTypeOrderByAnnotations(int layer) {
        return manager.getObjects()
                .filter(packageManager::isOwnPackage)
                .filter(manager::isNotOperationType)
                .filter(manager::classNotExists)
                .filter(manager::isNotContainerType)
                .filter(objectTypeDefinitionContext -> !objectTypeDefinitionContext.name().getText().equals(PAGE_INFO_NAME))
                .map(objectTypeDefinitionContext -> {
                            TypeSpec.Builder builder = TypeSpec.annotationBuilder(objectTypeDefinitionContext.name().getText().concat(ORDER_BY_SUFFIX) + layer)
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
                                                            manager.isScalar(manager.getFieldTypeName(fieldDefinitionContext.type())) ||
                                                                    manager.isEnum(manager.getFieldTypeName(fieldDefinitionContext.type()))
                                                    )
                                                    .filter(fieldDefinitionContext -> manager.isNotFunctionField(objectTypeDefinitionContext.name().getText(), fieldDefinitionContext.name().getText()))
                                                    .map(fieldDefinitionContext ->
                                                            MethodSpec.methodBuilder(fieldDefinitionContext.name().getText())
                                                                    .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                                                                    .returns(
                                                                            manager.getEnum("Sort")
                                                                                    .map(packageManager::getClassName)
                                                                                    .map(TYPE_NAME_UTIL::toClassName)
                                                                                    .orElseGet(() -> ClassName.get(graphQLConfig.getEnumTypePackageName(), "Sort"))
                                                                    )
                                                                    .defaultValue(CodeBlock.of("Sort.ASC"))
                                                                    .build()
                                                    )
                                                    .collect(Collectors.toList())
                                    );
                            if (layer < graphQLConfig.getInputLayers() - 1) {
                                builder.addMethods(
                                        manager.getFields(objectTypeDefinitionContext.name().getText())
                                                .filter(fieldDefinitionContext -> manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type())))
                                                .filter(fieldDefinitionContext -> manager.isNotContainerType(fieldDefinitionContext.type()))
                                                .filter(fieldDefinitionContext -> manager.isNotConnectionField(objectTypeDefinitionContext.name().getText(), fieldDefinitionContext.name().getText()))
                                                .filter(fieldDefinitionContext -> !fieldDefinitionContext.name().getText().endsWith(AGGREGATE_SUFFIX))
                                                .map(fieldDefinitionContext ->
                                                        MethodSpec.methodBuilder(fieldDefinitionContext.name().getText())
                                                                .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                                                                .returns(ClassName.get(graphQLConfig.getAnnotationPackageName(), manager.getFieldTypeName(fieldDefinitionContext.type()).concat(ORDER_BY_SUFFIX) + (layer + 1)))
                                                                .defaultValue(
                                                                        CodeBlock.of(
                                                                                "@$T",
                                                                                ClassName.get(graphQLConfig.getAnnotationPackageName(), manager.getFieldTypeName(fieldDefinitionContext.type()).concat(ORDER_BY_SUFFIX) + (layer + 1))
                                                                        )
                                                                )
                                                                .build()


                                                )
                                                .collect(Collectors.toList())
                                );
                            }
                            Logger.info("annotation {}OrderBy{} build success", objectTypeDefinitionContext.name().getText(), layer);
                            return builder.build();
                        }
                );
    }
}
