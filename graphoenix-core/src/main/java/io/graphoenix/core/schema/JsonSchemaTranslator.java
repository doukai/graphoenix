package io.graphoenix.core.schema;

import com.google.common.base.CaseFormat;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.error.GraphQLErrorType;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.*;
import jakarta.json.spi.JsonProvider;

import java.io.StringWriter;
import java.util.Optional;
import java.util.stream.Stream;

import static io.graphoenix.core.utils.DocumentUtil.DOCUMENT_UTIL;
import static io.graphoenix.core.utils.ValidationUtil.VALIDATION_UTIL;
import static io.graphoenix.spi.constant.Hammurabi.LIST_INPUT_NAME;
import static io.graphoenix.spi.constant.Hammurabi.WHERE_INPUT_NAME;
import static jakarta.json.JsonValue.EMPTY_JSON_OBJECT;
import static jakarta.json.JsonValue.TRUE;

@ApplicationScoped
public class JsonSchemaTranslator {

    private final JsonProvider jsonProvider;
    private final IGraphQLDocumentManager manager;

    @Inject
    public JsonSchemaTranslator(IGraphQLDocumentManager manager, JsonProvider jsonProvider) {
        this.manager = manager;
        this.jsonProvider = jsonProvider;
    }

    public String objectToJsonSchemaString(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext) {
        StringWriter stringWriter = new StringWriter();
        jsonProvider.createWriter(stringWriter).write(inputObjectToJsonSchema(inputObjectTypeDefinitionContext));
        return stringWriter.toString();
    }

    public String operationObjectToJsonSchemaString(GraphqlParser.OperationTypeDefinitionContext operationTypeDefinitionContext) {
        StringWriter stringWriter = new StringWriter();
        jsonProvider.createWriter(stringWriter).write(operationObjectToJsonSchema(operationTypeDefinitionContext));
        return stringWriter.toString();
    }

    public String operationObjectFieldArgumentsToJsonSchemaString(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext, GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        StringWriter stringWriter = new StringWriter();
        jsonProvider.createWriter(stringWriter).write(operationObjectFieldArgumentsToJsonSchema(objectTypeDefinitionContext, fieldDefinitionContext));
        return stringWriter.toString();
    }

    public String operationObjectFieldUpdateByIdArgumentsToJsonSchemaString(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext, GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        StringWriter stringWriter = new StringWriter();
        jsonProvider.createWriter(stringWriter).write(operationObjectFieldUpdateByIdArgumentsToJsonSchema(objectTypeDefinitionContext, fieldDefinitionContext));
        return stringWriter.toString();
    }

    public String operationObjectFieldUpdateByWhereArgumentsToJsonSchemaString(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext, GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        StringWriter stringWriter = new StringWriter();
        jsonProvider.createWriter(stringWriter).write(operationObjectFieldUpdateByWhereArgumentsToJsonSchema(objectTypeDefinitionContext, fieldDefinitionContext));
        return stringWriter.toString();
    }

    public String operationObjectFieldListArgumentsToJsonSchemaString(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext, GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        StringWriter stringWriter = new StringWriter();
        jsonProvider.createWriter(stringWriter).write(operationObjectFieldListArgumentsToJsonSchema(objectTypeDefinitionContext, fieldDefinitionContext));
        return stringWriter.toString();
    }

    public JsonValue inputObjectToJsonSchema(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext) {
        JsonObjectBuilder jsonSchemaBuilder = VALIDATION_UTIL.getValidationDirectiveContext(inputObjectTypeDefinitionContext.directives())
                .map(this::buildValidation)
                .orElseGet(jsonProvider::createObjectBuilder);
        JsonObjectBuilder builder = jsonSchemaBuilder.add("$id", jsonProvider.createValue("#".concat(inputObjectTypeDefinitionContext.name().getText())))
                .add("type", jsonProvider.createValue("object"))
                .add("properties", inputObjectToProperties(inputObjectTypeDefinitionContext))
                .add("additionalProperties", TRUE)
                .add("required", buildRequired(inputObjectTypeDefinitionContext));
        return builder.build();
    }

    public JsonValue operationObjectToJsonSchema(GraphqlParser.OperationTypeDefinitionContext operationTypeDefinitionContext) {
        GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext = manager.getObject(operationTypeDefinitionContext.typeName().name().getText())
                .orElseThrow(() -> new GraphQLErrors(GraphQLErrorType.TYPE_NOT_EXIST.bind(operationTypeDefinitionContext.typeName().name().getText())));
        JsonObjectBuilder jsonSchemaBuilder = jsonProvider.createObjectBuilder();

        JsonObjectBuilder builder = jsonSchemaBuilder.add("$id", jsonProvider.createValue("#".concat(objectTypeDefinitionContext.name().getText())))
                .add("type", jsonProvider.createValue("object"))
                .add("additionalProperties", TRUE);

        JsonObjectBuilder propertiesBuilder = jsonProvider.createObjectBuilder();
        objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                .filter(fieldDefinitionContext -> fieldDefinitionContext.argumentsDefinition() != null)
                .forEach(fieldDefinitionContext -> {
                            if (operationTypeDefinitionContext.operationType().MUTATION() != null && manager.isNotInvokeField(fieldDefinitionContext)) {
                                String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
                                JsonArrayBuilder jsonArrayBuilder = jsonProvider.createArrayBuilder()
                                        .add(
                                                jsonProvider.createObjectBuilder().add(
                                                        "$ref", fieldTypeName
                                                                .concat("Input")
                                                )
                                        )
                                        .add(
                                                jsonProvider.createObjectBuilder().add(
                                                        "$ref",
                                                        objectTypeDefinitionContext.name().getText()
                                                                .concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, fieldDefinitionContext.name().getText()))
                                                                .concat("UpdateById")
                                                )
                                        )
                                        .add(
                                                jsonProvider.createObjectBuilder().add(
                                                        "$ref", objectTypeDefinitionContext.name().getText()
                                                                .concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, fieldDefinitionContext.name().getText()))
                                                                .concat("UpdateByWhere")
                                                )
                                        );
                                if (manager.fieldTypeIsList(fieldDefinitionContext.type())) {
                                    jsonArrayBuilder.add(
                                            jsonProvider.createObjectBuilder().add(
                                                    "$ref", objectTypeDefinitionContext.name().getText()
                                                            .concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, fieldDefinitionContext.name().getText()))
                                            )
                                    );
                                }
                                propertiesBuilder.add(
                                        fieldDefinitionContext.name().getText(),
                                        jsonProvider.createObjectBuilder().add("anyOf", jsonArrayBuilder)
                                );
                            } else {
                                propertiesBuilder.add(
                                        fieldDefinitionContext.name().getText(),
                                        jsonProvider.createObjectBuilder()
                                                .add("$ref", objectTypeDefinitionContext.name().getText().concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, fieldDefinitionContext.name().getText())))
                                );
                            }
                        }
                );
        builder.add("properties", propertiesBuilder);
        ;
        return builder.build();
    }

    public JsonValue operationObjectFieldArgumentsToJsonSchema(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext, GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        JsonObjectBuilder jsonSchemaBuilder = VALIDATION_UTIL.getValidationDirectiveContext(fieldDefinitionContext.directives())
                .map(this::buildValidation)
                .orElseGet(jsonProvider::createObjectBuilder);
        JsonObjectBuilder builder = jsonSchemaBuilder.add(
                "$id",
                jsonProvider.createValue(
                        "#".concat(objectTypeDefinitionContext.name().getText())
                                .concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, fieldDefinitionContext.name().getText()))
                )
        )
                .add("type", jsonProvider.createValue("object"))
                .add("properties", fieldArgumentsToProperties(fieldDefinitionContext))
                .add("additionalProperties", TRUE);
        if (fieldDefinitionContext.argumentsDefinition() != null) {
            builder.add("required", buildRequired(fieldDefinitionContext.argumentsDefinition()));
        }
        return builder.build();
    }

    public JsonValue operationObjectFieldUpdateByIdArgumentsToJsonSchema(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext, GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        JsonObjectBuilder jsonSchemaBuilder = VALIDATION_UTIL.getValidationDirectiveContext(fieldDefinitionContext.directives())
                .map(this::buildValidation)
                .orElseGet(jsonProvider::createObjectBuilder);
        JsonObjectBuilder builder = jsonSchemaBuilder.add(
                "$id",
                jsonProvider.createValue(
                        "#".concat(objectTypeDefinitionContext.name().getText())
                                .concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, fieldDefinitionContext.name().getText()))
                                .concat("UpdateById")
                )
        )
                .add("type", jsonProvider.createValue("object"))
                .add("properties", fieldArgumentsToUpdateByIdProperties(fieldDefinitionContext))
                .add("additionalProperties", TRUE);
        return builder.build();
    }

    public JsonValue operationObjectFieldUpdateByWhereArgumentsToJsonSchema(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext, GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        JsonObjectBuilder jsonSchemaBuilder = VALIDATION_UTIL.getValidationDirectiveContext(fieldDefinitionContext.directives())
                .map(this::buildValidation)
                .orElseGet(jsonProvider::createObjectBuilder);
        JsonObjectBuilder builder = jsonSchemaBuilder.add(
                "$id",
                jsonProvider.createValue(
                        "#".concat(objectTypeDefinitionContext.name().getText())
                                .concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, fieldDefinitionContext.name().getText()))
                                .concat("UpdateByWhere")
                )
        )
                .add("type", jsonProvider.createValue("object"))
                .add("properties", fieldArgumentsToUpdateByWhereProperties(fieldDefinitionContext))
                .add("additionalProperties", TRUE);
        return builder.build();
    }

    public JsonValue operationObjectFieldListArgumentsToJsonSchema(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext, GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        JsonObjectBuilder jsonSchemaBuilder = VALIDATION_UTIL.getValidationDirectiveContext(fieldDefinitionContext.directives())
                .map(this::buildValidation)
                .orElseGet(jsonProvider::createObjectBuilder);
        JsonObjectBuilder builder = jsonSchemaBuilder.add(
                "$id",
                jsonProvider.createValue(
                        "#".concat(objectTypeDefinitionContext.name().getText())
                                .concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, fieldDefinitionContext.name().getText()))
                )
        )
                .add("type", jsonProvider.createValue("object"))
                .add("properties", fieldArgumentsToListProperties(fieldDefinitionContext))
                .add("additionalProperties", TRUE);
        return builder.build();
    }

    protected JsonArrayBuilder buildRequired(GraphqlParser.ArgumentsDefinitionContext argumentsDefinitionContext) {
        JsonArrayBuilder requiredBuilder = jsonProvider.createArrayBuilder();
        argumentsDefinitionContext.inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> inputValueDefinitionContext.type().nonNullType() != null)
                .forEach(inputValueDefinitionContext -> requiredBuilder.add(inputValueDefinitionContext.name().getText()));
        return requiredBuilder;
    }

    protected JsonArrayBuilder buildRequired(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext) {
        JsonArrayBuilder requiredBuilder = jsonProvider.createArrayBuilder();
        inputObjectTypeDefinitionContext.inputObjectValueDefinitions().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> inputValueDefinitionContext.type().nonNullType() != null)
                .forEach(inputValueDefinitionContext -> requiredBuilder.add(inputValueDefinitionContext.name().getText()));
        return requiredBuilder;
    }

    protected JsonObjectBuilder fieldArgumentsToProperties(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        JsonObjectBuilder propertiesBuilder = jsonProvider.createObjectBuilder();
        Stream.ofNullable(fieldDefinitionContext.argumentsDefinition()).flatMap(argumentsDefinitionContext -> argumentsDefinitionContext.inputValueDefinition().stream())
                .forEach(inputValueDefinitionContext ->
                        propertiesBuilder.add(inputValueDefinitionContext.name().getText(), fieldToProperty(inputValueDefinitionContext.type(), VALIDATION_UTIL.getValidationDirectiveContext(inputValueDefinitionContext.directives()).orElse(null)))
                );
        return propertiesBuilder;
    }

    protected JsonObjectBuilder fieldArgumentsToUpdateByIdProperties(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
        Optional<String> objectTypeIDFieldName = manager.getObjectTypeIDFieldName(fieldTypeName);
        JsonObjectBuilder propertiesBuilder = jsonProvider.createObjectBuilder();
        Stream.ofNullable(fieldDefinitionContext.argumentsDefinition()).flatMap(argumentsDefinitionContext -> argumentsDefinitionContext.inputValueDefinition().stream())
                .filter(inputValueDefinitionContext -> !inputValueDefinitionContext.name().getText().equals(LIST_INPUT_NAME))
                .filter(inputValueDefinitionContext -> !inputValueDefinitionContext.name().getText().equals(WHERE_INPUT_NAME))
                .forEach(inputValueDefinitionContext -> {
                            if (objectTypeIDFieldName.isPresent() && objectTypeIDFieldName.get().equals(inputValueDefinitionContext.name().getText())) {
                                propertiesBuilder.add(inputValueDefinitionContext.name().getText(), jsonProvider.createObjectBuilder().add("type", "string"));
                            } else {
                                propertiesBuilder.add(inputValueDefinitionContext.name().getText(), fieldToProperty(inputValueDefinitionContext.type(), VALIDATION_UTIL.getValidationDirectiveContext(inputValueDefinitionContext.directives()).orElse(null)));
                            }
                        }
                );
        propertiesBuilder.add("update", jsonProvider.createObjectBuilder().add("const", true));
        return propertiesBuilder;
    }

    protected JsonObjectBuilder fieldArgumentsToUpdateByWhereProperties(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
        Optional<String> objectTypeIDFieldName = manager.getObjectTypeIDFieldName(fieldTypeName);
        JsonObjectBuilder propertiesBuilder = jsonProvider.createObjectBuilder();
        Stream.ofNullable(fieldDefinitionContext.argumentsDefinition()).flatMap(argumentsDefinitionContext -> argumentsDefinitionContext.inputValueDefinition().stream())
                .filter(inputValueDefinitionContext -> !inputValueDefinitionContext.name().getText().equals(LIST_INPUT_NAME))
                .filter(inputValueDefinitionContext -> objectTypeIDFieldName.isEmpty() || !objectTypeIDFieldName.get().equals(inputValueDefinitionContext.name().getText()))
                .forEach(inputValueDefinitionContext -> {
                            if (WHERE_INPUT_NAME.equals(inputValueDefinitionContext.name().getText())) {
                                JsonObjectBuilder propertyBuilder = VALIDATION_UTIL.getValidationDirectiveContext(inputValueDefinitionContext.directives())
                                        .map(this::buildValidation)
                                        .orElseGet(jsonProvider::createObjectBuilder);
                                if (inputValueDefinitionContext.type().nonNullType() != null) {
                                    propertiesBuilder.add(inputValueDefinitionContext.name().getText(), buildType(inputValueDefinitionContext.type().nonNullType().typeName(), propertyBuilder));
                                } else {
                                    propertiesBuilder.add(inputValueDefinitionContext.name().getText(), buildType(inputValueDefinitionContext.type().typeName(), propertyBuilder));
                                }
                            } else {
                                propertiesBuilder.add(inputValueDefinitionContext.name().getText(), fieldToProperty(inputValueDefinitionContext.type(), VALIDATION_UTIL.getValidationDirectiveContext(inputValueDefinitionContext.directives()).orElse(null)));
                            }
                        }
                );
        propertiesBuilder.add("update", jsonProvider.createObjectBuilder().add("const", true));
        return propertiesBuilder;
    }

    protected JsonObjectBuilder fieldArgumentsToListProperties(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        JsonObjectBuilder propertiesBuilder = jsonProvider.createObjectBuilder();
        Stream.ofNullable(fieldDefinitionContext.argumentsDefinition()).flatMap(argumentsDefinitionContext -> argumentsDefinitionContext.inputValueDefinition().stream())
                .filter(inputValueDefinitionContext -> inputValueDefinitionContext.name().getText().equals(LIST_INPUT_NAME))
                .forEach(inputValueDefinitionContext ->
                        propertiesBuilder.add(inputValueDefinitionContext.name().getText(), fieldToProperty(inputValueDefinitionContext.type(), VALIDATION_UTIL.getValidationDirectiveContext(inputValueDefinitionContext.directives()).orElse(null)))
                );
        return propertiesBuilder;
    }

    protected JsonObjectBuilder inputObjectToProperties(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext) {
        JsonObjectBuilder propertiesBuilder = jsonProvider.createObjectBuilder();
        inputObjectTypeDefinitionContext.inputObjectValueDefinitions().inputValueDefinition()
                .forEach(inputValueDefinitionContext ->
                        propertiesBuilder.add(inputValueDefinitionContext.name().getText(), fieldToProperty(inputValueDefinitionContext.type(), VALIDATION_UTIL.getValidationDirectiveContext(inputValueDefinitionContext.directives()).orElse(null)))
                );
        return propertiesBuilder;
    }

    protected JsonObjectBuilder fieldToProperty(GraphqlParser.TypeContext typeContext, GraphqlParser.DirectiveContext directiveContext) {
        JsonObjectBuilder propertyBuilder = Optional.ofNullable(directiveContext)
                .map(this::buildValidation)
                .orElseGet(jsonProvider::createObjectBuilder);
        if (typeContext.listType() != null) {
            propertyBuilder.add("type", jsonProvider.createValue("array"));
            GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext = Optional.ofNullable(directiveContext)
                    .flatMap(arrayDirectiveContext -> VALIDATION_UTIL.getValidationObjectArgument(arrayDirectiveContext, "items"))
                    .orElse(null);
            propertyBuilder.add("items", fieldToProperty(typeContext.listType().type(), objectValueWithVariableContext));
            return propertyBuilder;
        } else if (typeContext.nonNullType() != null) {
            if (typeContext.nonNullType().listType() != null) {
                propertyBuilder.add("type", jsonProvider.createValue("array"));
                GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext = Optional.ofNullable(directiveContext)
                        .flatMap(arrayDirectiveContext -> VALIDATION_UTIL.getValidationObjectArgument(arrayDirectiveContext, "items"))
                        .orElse(null);
                propertyBuilder.add("items", fieldToProperty(typeContext.nonNullType().listType().type(), objectValueWithVariableContext));
                return propertyBuilder;
            } else {
                return buildType(typeContext.nonNullType().typeName(), propertyBuilder);
            }
        } else {
            return buildNullableType(buildType(typeContext.typeName(), propertyBuilder));
        }
    }

    protected JsonObjectBuilder fieldToProperty(GraphqlParser.TypeContext typeContext, GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {
        JsonObjectBuilder propertyBuilder = Optional.ofNullable(objectValueWithVariableContext)
                .map(this::buildValidation)
                .orElseGet(jsonProvider::createObjectBuilder);
        if (typeContext.listType() != null) {
            propertyBuilder.add("type", jsonProvider.createValue("array"));
            GraphqlParser.ObjectValueWithVariableContext subObjectValueWithVariableContext = Optional.ofNullable(objectValueWithVariableContext)
                    .flatMap(arrayObjectValueWithVariableContext -> VALIDATION_UTIL.getValidationObjectArgument(arrayObjectValueWithVariableContext, "items"))
                    .orElse(null);
            propertyBuilder.add("items", fieldToProperty(typeContext.listType().type(), subObjectValueWithVariableContext));
            return propertyBuilder;
        } else if (typeContext.nonNullType() != null) {
            if (typeContext.nonNullType().listType() != null) {
                propertyBuilder.add("type", jsonProvider.createValue("array"));
                GraphqlParser.ObjectValueWithVariableContext subObjectValueWithVariableContext = Optional.ofNullable(objectValueWithVariableContext)
                        .flatMap(arrayObjectValueWithVariableContext -> VALIDATION_UTIL.getValidationObjectArgument(arrayObjectValueWithVariableContext, "items"))
                        .orElse(null);
                propertyBuilder.add("items", fieldToProperty(typeContext.nonNullType().listType().type(), subObjectValueWithVariableContext));
                return propertyBuilder;
            } else {
                return buildType(typeContext.nonNullType().typeName(), propertyBuilder);
            }
        } else {
            return buildNullableType(buildType(typeContext.typeName(), propertyBuilder));
        }
    }

    protected JsonObjectBuilder buildType(GraphqlParser.TypeNameContext typeNameContext, JsonObjectBuilder jsonObjectBuilder) {
        String fieldTypeName = typeNameContext.getText();
        if (manager.isScalar(fieldTypeName)) {
            switch (fieldTypeName) {
                case "ID":
                case "String":
                case "Date":
                case "Time":
                case "DateTime":
                case "Timestamp":
                    jsonObjectBuilder.add("type", jsonProvider.createValue("string"));
                    break;
                case "Boolean":
                    jsonObjectBuilder.add("type", jsonProvider.createValue("boolean"));
                    break;
                case "Int":
                case "BigInteger":
                    jsonObjectBuilder.add("type", jsonProvider.createValue("integer"));
                    break;
                case "Float":
                case "BigDecimal":
                    jsonObjectBuilder.add("type", jsonProvider.createValue("number"));
                    break;
            }
        } else if (manager.isEnum(fieldTypeName)) {
            JsonArrayBuilder enumValuesBuilder = jsonProvider.createArrayBuilder();
            manager.getEnum(fieldTypeName)
                    .ifPresent(enumTypeDefinitionContext -> {
                                enumTypeDefinitionContext.enumValueDefinitions().enumValueDefinition()
                                        .forEach(enumValueDefinitionContext -> enumValuesBuilder.add(enumValueDefinitionContext.enumValue().getText()));
                                jsonObjectBuilder.add("enum", enumValuesBuilder);
                            }
                    );
        } else if (manager.isInputObject(fieldTypeName)) {
            jsonObjectBuilder.add("$ref", jsonProvider.createValue(fieldTypeName));
        }
        return jsonObjectBuilder;
    }

    protected JsonObjectBuilder buildNullableType(JsonObjectBuilder typeBuilder) {
        return jsonProvider.createObjectBuilder()
                .add("anyOf",
                        jsonProvider.createArrayBuilder()
                                .add(typeBuilder)
                                .add(jsonProvider.createObjectBuilder().add("type", jsonProvider.createValue("null")))
                );
    }

    protected JsonObjectBuilder buildValidation(GraphqlParser.DirectiveContext directiveContext) {
        JsonObjectBuilder validationBuilder = jsonProvider.createObjectBuilder();

        VALIDATION_UTIL.getValidationIntArgument(directiveContext, "minLength")
                .ifPresent(minLength -> validationBuilder.add("minLength", minLength));
        VALIDATION_UTIL.getValidationIntArgument(directiveContext, "maxLength")
                .ifPresent(maxLength -> validationBuilder.add("maxLength", maxLength));
        VALIDATION_UTIL.getValidationStringArgument(directiveContext, "pattern")
                .ifPresent(pattern -> validationBuilder.add("pattern", pattern));
        VALIDATION_UTIL.getValidationStringArgument(directiveContext, "format")
                .ifPresent(format -> validationBuilder.add("format", format));
        VALIDATION_UTIL.getValidationStringArgument(directiveContext, "contentMediaType")
                .ifPresent(contentMediaType -> validationBuilder.add("contentMediaType", contentMediaType));
        VALIDATION_UTIL.getValidationStringArgument(directiveContext, "contentEncoding")
                .ifPresent(contentEncoding -> validationBuilder.add("contentEncoding", contentEncoding));

        VALIDATION_UTIL.getValidationFloatArgument(directiveContext, "minimum")
                .ifPresent(minimum -> validationBuilder.add("minimum", minimum));
        VALIDATION_UTIL.getValidationFloatArgument(directiveContext, "exclusiveMinimum")
                .ifPresent(exclusiveMinimum -> validationBuilder.add("exclusiveMinimum", exclusiveMinimum));
        VALIDATION_UTIL.getValidationFloatArgument(directiveContext, "maximum")
                .ifPresent(maximum -> validationBuilder.add("maximum", maximum));
        VALIDATION_UTIL.getValidationFloatArgument(directiveContext, "exclusiveMaximum")
                .ifPresent(exclusiveMaximum -> validationBuilder.add("exclusiveMaximum", exclusiveMaximum));
        VALIDATION_UTIL.getValidationFloatArgument(directiveContext, "multipleOf")
                .ifPresent(multipleOf -> validationBuilder.add("multipleOf", multipleOf));

        VALIDATION_UTIL.getValidationStringArgument(directiveContext, "const")
                .ifPresent(constValue -> validationBuilder.add("const", constValue));

        VALIDATION_UTIL.getValidationArrayArgument(directiveContext, "allOf")
                .ifPresent(arrayValueWithVariableContext -> {
                            JsonArrayBuilder allOfBuilder = jsonProvider.createArrayBuilder();
                            arrayValueWithVariableContext.valueWithVariable().stream()
                                    .filter(valueWithVariableContext -> valueWithVariableContext.objectValueWithVariable() != null)
                                    .forEach(valueWithVariableContext -> allOfBuilder.add(buildValidation(valueWithVariableContext.objectValueWithVariable())));
                            validationBuilder.add("allOf", allOfBuilder);
                        }
                );

        VALIDATION_UTIL.getValidationArrayArgument(directiveContext, "anyOf")
                .ifPresent(arrayValueWithVariableContext -> {
                            JsonArrayBuilder anyOfBuilder = jsonProvider.createArrayBuilder();
                            arrayValueWithVariableContext.valueWithVariable().stream()
                                    .filter(valueWithVariableContext -> valueWithVariableContext.objectValueWithVariable() != null)
                                    .forEach(valueWithVariableContext -> anyOfBuilder.add(buildValidation(valueWithVariableContext.objectValueWithVariable())));
                            validationBuilder.add("anyOf", anyOfBuilder);
                        }
                );

        VALIDATION_UTIL.getValidationArrayArgument(directiveContext, "anyOf")
                .ifPresent(arrayValueWithVariableContext -> {
                            JsonArrayBuilder oneOfBuilder = jsonProvider.createArrayBuilder();
                            arrayValueWithVariableContext.valueWithVariable().stream()
                                    .filter(valueWithVariableContext -> valueWithVariableContext.objectValueWithVariable() != null)
                                    .forEach(valueWithVariableContext -> oneOfBuilder.add(buildValidation(valueWithVariableContext.objectValueWithVariable())));
                            validationBuilder.add("anyOf", oneOfBuilder);
                        }
                );

        VALIDATION_UTIL.getValidationObjectArgument(directiveContext, "not")
                .ifPresent(not -> validationBuilder.add("not", buildValidation(not)));

        VALIDATION_UTIL.getValidationArrayArgument(directiveContext, "properties")
                .ifPresent(arrayValueWithVariableContext -> validationBuilder.add("properties", buildProperties(arrayValueWithVariableContext)));

        VALIDATION_UTIL.getValidationObjectArgument(directiveContext, "if")
                .ifPresent(ifValidation -> validationBuilder.add("if", buildValidation(ifValidation)));

        VALIDATION_UTIL.getValidationObjectArgument(directiveContext, "then")
                .ifPresent(thenValidation -> validationBuilder.add("then", buildValidation(thenValidation)));

        VALIDATION_UTIL.getValidationObjectArgument(directiveContext, "else")
                .ifPresent(elseValidation -> validationBuilder.add("else", buildValidation(elseValidation)));

        VALIDATION_UTIL.getValidationArrayArgument(directiveContext, "dependentRequired")
                .ifPresent(arrayValueWithVariableContext -> validationBuilder.add("dependentRequired", buildDependentRequired(arrayValueWithVariableContext)));

        return validationBuilder;
    }

    protected JsonObjectBuilder buildValidation(GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {
        JsonObjectBuilder validationBuilder = jsonProvider.createObjectBuilder();

        VALIDATION_UTIL.getValidationIntArgument(objectValueWithVariableContext, "minLength")
                .ifPresent(minLength -> validationBuilder.add("minLength", minLength));
        VALIDATION_UTIL.getValidationIntArgument(objectValueWithVariableContext, "maxLength")
                .ifPresent(maxLength -> validationBuilder.add("maxLength", maxLength));
        VALIDATION_UTIL.getValidationStringArgument(objectValueWithVariableContext, "pattern")
                .ifPresent(pattern -> validationBuilder.add("pattern", pattern));
        VALIDATION_UTIL.getValidationStringArgument(objectValueWithVariableContext, "format")
                .ifPresent(format -> validationBuilder.add("format", format));
        VALIDATION_UTIL.getValidationStringArgument(objectValueWithVariableContext, "contentMediaType")
                .ifPresent(contentMediaType -> validationBuilder.add("contentMediaType", contentMediaType));
        VALIDATION_UTIL.getValidationStringArgument(objectValueWithVariableContext, "contentEncoding")
                .ifPresent(contentEncoding -> validationBuilder.add("contentEncoding", contentEncoding));

        VALIDATION_UTIL.getValidationFloatArgument(objectValueWithVariableContext, "minimum")
                .ifPresent(minimum -> validationBuilder.add("minimum", minimum));
        VALIDATION_UTIL.getValidationFloatArgument(objectValueWithVariableContext, "exclusiveMinimum")
                .ifPresent(exclusiveMinimum -> validationBuilder.add("exclusiveMinimum", exclusiveMinimum));
        VALIDATION_UTIL.getValidationFloatArgument(objectValueWithVariableContext, "maximum")
                .ifPresent(maximum -> validationBuilder.add("maximum", maximum));
        VALIDATION_UTIL.getValidationFloatArgument(objectValueWithVariableContext, "exclusiveMaximum")
                .ifPresent(exclusiveMaximum -> validationBuilder.add("exclusiveMaximum", exclusiveMaximum));
        VALIDATION_UTIL.getValidationFloatArgument(objectValueWithVariableContext, "multipleOf")
                .ifPresent(multipleOf -> validationBuilder.add("multipleOf", multipleOf));

        VALIDATION_UTIL.getValidationStringArgument(objectValueWithVariableContext, "const")
                .ifPresent(constValue -> validationBuilder.add("const", constValue));

        VALIDATION_UTIL.getValidationArrayArgument(objectValueWithVariableContext, "allOf")
                .ifPresent(arrayValueWithVariableContext -> {
                            JsonArrayBuilder allOfBuilder = jsonProvider.createArrayBuilder();
                            arrayValueWithVariableContext.valueWithVariable().stream()
                                    .filter(valueWithVariableContext -> valueWithVariableContext.objectValueWithVariable() != null)
                                    .forEach(valueWithVariableContext -> allOfBuilder.add(buildValidation(valueWithVariableContext.objectValueWithVariable())));
                            validationBuilder.add("allOf", allOfBuilder);
                        }
                );

        VALIDATION_UTIL.getValidationArrayArgument(objectValueWithVariableContext, "anyOf")
                .ifPresent(arrayValueWithVariableContext -> {
                            JsonArrayBuilder anyOfBuilder = jsonProvider.createArrayBuilder();
                            arrayValueWithVariableContext.valueWithVariable().stream()
                                    .filter(valueWithVariableContext -> valueWithVariableContext.objectValueWithVariable() != null)
                                    .forEach(valueWithVariableContext -> anyOfBuilder.add(buildValidation(valueWithVariableContext.objectValueWithVariable())));
                            validationBuilder.add("anyOf", anyOfBuilder);
                        }
                );

        VALIDATION_UTIL.getValidationArrayArgument(objectValueWithVariableContext, "anyOf")
                .ifPresent(arrayValueWithVariableContext -> {
                            JsonArrayBuilder oneOfBuilder = jsonProvider.createArrayBuilder();
                            arrayValueWithVariableContext.valueWithVariable().stream()
                                    .filter(valueWithVariableContext -> valueWithVariableContext.objectValueWithVariable() != null)
                                    .forEach(valueWithVariableContext -> oneOfBuilder.add(buildValidation(valueWithVariableContext.objectValueWithVariable())));
                            validationBuilder.add("anyOf", oneOfBuilder);
                        }
                );

        VALIDATION_UTIL.getValidationObjectArgument(objectValueWithVariableContext, "not")
                .ifPresent(not -> validationBuilder.add("not", buildValidation(not)));

        VALIDATION_UTIL.getValidationArrayArgument(objectValueWithVariableContext, "properties")
                .ifPresent(arrayValueWithVariableContext -> validationBuilder.add("properties", buildProperties(arrayValueWithVariableContext)));

        VALIDATION_UTIL.getValidationObjectArgument(objectValueWithVariableContext, "if")
                .ifPresent(ifValidation -> validationBuilder.add("if", buildValidation(ifValidation)));

        VALIDATION_UTIL.getValidationObjectArgument(objectValueWithVariableContext, "then")
                .ifPresent(thenValidation -> validationBuilder.add("then", buildValidation(thenValidation)));

        VALIDATION_UTIL.getValidationObjectArgument(objectValueWithVariableContext, "else")
                .ifPresent(elseValidation -> validationBuilder.add("else", buildValidation(elseValidation)));

        VALIDATION_UTIL.getValidationArrayArgument(objectValueWithVariableContext, "dependentRequired")
                .ifPresent(arrayValueWithVariableContext -> validationBuilder.add("dependentRequired", buildDependentRequired(arrayValueWithVariableContext)));

        return validationBuilder;
    }

    protected JsonObjectBuilder buildProperties(GraphqlParser.ArrayValueWithVariableContext arrayValueWithVariableContext) {
        JsonObjectBuilder propertiesBuilder = jsonProvider.createObjectBuilder();
        arrayValueWithVariableContext.valueWithVariable().stream()
                .filter(property -> property.objectValueWithVariable() != null)
                .forEach(property ->
                        VALIDATION_UTIL.getValidationStringArgument(property.objectValueWithVariable(), "name")
                                .ifPresent(name ->
                                        VALIDATION_UTIL.getValidationObjectArgument(property.objectValueWithVariable(), "validation")
                                                .ifPresent(validation ->
                                                        propertiesBuilder.add(name, buildValidation(validation))
                                                )
                                )
                );
        return propertiesBuilder;
    }

    protected JsonObjectBuilder buildDependentRequired(GraphqlParser.ArrayValueWithVariableContext arrayValueWithVariableContext) {
        JsonObjectBuilder dependentRequiredBuilder = jsonProvider.createObjectBuilder();
        arrayValueWithVariableContext.valueWithVariable().stream()
                .filter(property -> property.objectValueWithVariable() != null)
                .forEach(property ->
                        VALIDATION_UTIL.getValidationStringArgument(property.objectValueWithVariable(), "name")
                                .ifPresent(name ->
                                        VALIDATION_UTIL.getValidationArrayArgument(property.objectValueWithVariable(), "required")
                                                .ifPresent(required -> {
                                                            JsonArrayBuilder jsonArrayBuilder = jsonProvider.createArrayBuilder();
                                                            required.valueWithVariable().stream()
                                                                    .filter(item -> item.StringValue() != null)
                                                                    .forEach(item -> jsonArrayBuilder.add(DOCUMENT_UTIL.getStringValue(item.StringValue())));
                                                            dependentRequiredBuilder.add(name, jsonArrayBuilder);
                                                        }
                                                )
                                )
                );
        return dependentRequiredBuilder;
    }
}
