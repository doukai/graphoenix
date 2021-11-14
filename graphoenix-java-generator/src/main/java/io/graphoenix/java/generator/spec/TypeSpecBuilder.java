package io.graphoenix.java.generator.spec;

import com.squareup.javapoet.*;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.java.generator.config.JavaGeneratorConfiguration;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;

import javax.annotation.Nonnull;
import javax.lang.model.element.Modifier;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TypeSpecBuilder {

    private final IGraphQLDocumentManager manager;
    private final JavaGeneratorConfiguration configuration;

    public TypeSpecBuilder(IGraphQLDocumentManager manager, JavaGeneratorConfiguration configuration) {
        this.manager = manager;
        this.configuration = configuration;
    }

    public TypeSpec buildClass(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        TypeSpec.Builder builder = TypeSpec.classBuilder(objectTypeDefinitionContext.name().getText())
                .addModifiers(Modifier.PUBLIC);
        objectTypeDefinitionContext.fieldsDefinition().fieldDefinition()
                .forEach(fieldDefinitionContext -> builder.addField(buildField(fieldDefinitionContext)));
        if (objectTypeDefinitionContext.implementsInterfaces() != null) {
            objectTypeDefinitionContext.implementsInterfaces().typeName()
                    .forEach(typeNameContext -> builder.addSuperinterface(ClassName.get(configuration.getInterfaceTypePackageName(), typeNameContext.name().getText())));
        }
        return builder.build();
    }

    public TypeSpec buildClass(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext) {
        TypeSpec.Builder builder = TypeSpec.classBuilder(inputObjectTypeDefinitionContext.name().getText())
                .addModifiers(Modifier.PUBLIC);
        inputObjectTypeDefinitionContext.inputObjectValueDefinitions().inputValueDefinition()
                .forEach(inputValueDefinitionContext -> builder.addField(buildField(inputValueDefinitionContext)));
        return builder.build();
    }

    public TypeSpec buildEnum(GraphqlParser.EnumTypeDefinitionContext enumTypeDefinitionContext) {
        TypeSpec.Builder builder = TypeSpec.enumBuilder(enumTypeDefinitionContext.name().getText())
                .addModifiers(Modifier.PUBLIC);
        enumTypeDefinitionContext.enumValueDefinitions().enumValueDefinition()
                .forEach(enumValueDefinitionContext -> builder.addEnumConstant(enumTypeDefinitionContext.name().getText()));
        return builder.build();
    }

    public TypeSpec buildInterface(GraphqlParser.InterfaceTypeDefinitionContext interfaceTypeDefinitionContext) {
        TypeSpec.Builder builder = TypeSpec.interfaceBuilder(interfaceTypeDefinitionContext.name().getText())
                .addModifiers(Modifier.PUBLIC);
        interfaceTypeDefinitionContext.fieldsDefinition().fieldDefinition()
                .forEach(fieldDefinitionContext -> builder.addField(buildField(fieldDefinitionContext)));
        return builder.build();
    }

    public TypeSpec buildAnnotation(GraphqlParser.DirectiveDefinitionContext directiveDefinitionContext) {
        TypeSpec.Builder builder = TypeSpec.annotationBuilder(directiveDefinitionContext.name().getText())
                .addModifiers(Modifier.PUBLIC);
        directiveDefinitionContext.argumentsDefinition().inputValueDefinition()
                .forEach(inputValueDefinitionContext -> builder.addField(buildArgument(inputValueDefinitionContext)));
        builder.addAnnotation(
                AnnotationSpec.builder(Retention.class)
                        .addMember("value", "$T.$L", RetentionPolicy.class, RetentionPolicy.SOURCE)
                        .build()
        );
        List<ElementType> elementTypeList = buildElementTypeList(directiveDefinitionContext.directiveLocations());
        CodeBlock.Builder codeBuilder = CodeBlock.builder();
        codeBuilder.add("{");
        elementTypeList.forEach(elementType -> codeBuilder.add("$T.$L", ElementType.class, elementType));
        codeBuilder.add("}");
        builder.addAnnotation(
                AnnotationSpec.builder(Target.class)
                        .addMember("value", codeBuilder.build())
                        .build()
        );
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
            builder.addAnnotation(Nonnull.class);
        }
        return builder.build();
    }

    public FieldSpec buildField(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        FieldSpec.Builder builder = FieldSpec.builder(buildType(inputValueDefinitionContext.type()), inputValueDefinitionContext.name().getText(), Modifier.PRIVATE);
        if (inputValueDefinitionContext.type().nonNullType() != null) {
            builder.addAnnotation(Nonnull.class);
        }
        return builder.build();
    }

    public FieldSpec buildArgument(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        FieldSpec.Builder builder = FieldSpec.builder(buildType(inputValueDefinitionContext.type()), inputValueDefinitionContext.name().getText(), Modifier.STATIC, Modifier.FINAL, Modifier.PRIVATE);
        if (inputValueDefinitionContext.type().nonNullType() != null) {
            builder.addAnnotation(Nonnull.class);
        }
        return builder.build();
    }

    public TypeName buildType(GraphqlParser.TypeContext typeContext) {
        if (typeContext.typeName() != null) {
            return buildType(typeContext.typeName().name());
        } else if (typeContext.listType() != null) {
            return buildType(typeContext.listType());
        } else if (typeContext.nonNullType() != null) {
            if (typeContext.nonNullType().typeName() != null) {
                return buildType(typeContext.nonNullType().typeName().name());
            } else if (typeContext.nonNullType().listType() != null) {
                return buildType(typeContext.nonNullType().listType());
            }
        }
        return null;
    }

    public TypeName buildType(GraphqlParser.NameContext nameContext) {
        if (manager.isScaLar(nameContext.getText())) {
            Optional<GraphqlParser.ScalarTypeDefinitionContext> scaLar = manager.getScaLar(nameContext.getText());
            if (scaLar.isPresent()) {
                return buildType(scaLar.get());
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
                return ClassName.get(configuration.getInputObjectTypePackageName(), inputObject.get().name().getText());
            }
        }
        return null;
    }

    public TypeName buildType(GraphqlParser.ListTypeContext listTypeContext) {
        return ArrayTypeName.of(buildType(listTypeContext.type()));
    }

    public TypeName buildType(GraphqlParser.ScalarTypeDefinitionContext scalarTypeDefinitionContext) {
        String name = scalarTypeDefinitionContext.name().getText();
        switch (name) {
            case "String":
                return TypeName.get(String.class);
            case "Boolean":
                return TypeName.get(Boolean.class);
            case "Int":
                return TypeName.get(Integer.class);
            case "Float":
                return TypeName.get(Float.class);
        }
        return null;
    }
}
