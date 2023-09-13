package io.graphoenix.core.schema;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.error.GraphQLErrorType;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;
import jakarta.json.spi.JsonProvider;

import java.io.StringWriter;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static io.graphoenix.core.error.GraphQLErrorType.TYPE_NOT_EXIST;
import static io.graphoenix.core.utils.DocumentUtil.DOCUMENT_UTIL;
import static io.graphoenix.core.utils.NameUtil.NAME_UTIL;
import static io.graphoenix.core.utils.ValidationUtil.VALIDATION_UTIL;
import static io.graphoenix.spi.constant.Hammurabi.ARGUMENTS_SUFFIX;
import static io.graphoenix.spi.constant.Hammurabi.INPUT_SUFFIX;
import static io.graphoenix.spi.constant.Hammurabi.LIST_INPUT_NAME;
import static io.graphoenix.spi.constant.Hammurabi.MUTATION_TYPE_NAME;
import static io.graphoenix.spi.constant.Hammurabi.QUERY_TYPE_NAME;
import static io.graphoenix.spi.constant.Hammurabi.SUBSCRIPTION_TYPE_NAME;
import static io.graphoenix.spi.constant.Hammurabi.WHERE_INPUT_NAME;
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

    public Stream<Map.Entry<String, String>> objectToJsonSchemaStringStream(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext) {
        return inputObjectToJsonSchema(inputObjectTypeDefinitionContext)
                .map(jsonValue -> {
                            StringWriter stringWriter = new StringWriter();
                            jsonProvider.createWriter(stringWriter).write(jsonValue);
                            return new AbstractMap.SimpleEntry<>(jsonValue.asJsonObject().getString("$id").substring(1), stringWriter.toString());
                        }
                );
    }

    public Map.Entry<String, String> operationObjectToJsonSchemaString(GraphqlParser.OperationTypeDefinitionContext operationTypeDefinitionContext) {
        JsonValue jsonValue = operationObjectToJsonSchema(operationTypeDefinitionContext);
        StringWriter stringWriter = new StringWriter();
        jsonProvider.createWriter(stringWriter).write(jsonValue);
        return new AbstractMap.SimpleEntry<>(jsonValue.asJsonObject().getString("$id").substring(1), stringWriter.toString());
    }

    public Stream<JsonValue> inputObjectToJsonSchema(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext) {
        String mutationTypeName = manager.getMutationOperationTypeName().orElse(MUTATION_TYPE_NAME);
        if (inputObjectTypeDefinitionContext.name().getText().endsWith(INPUT_SUFFIX)) {
            JsonObjectBuilder updateBuilder = VALIDATION_UTIL.getValidationDirectiveContext(inputObjectTypeDefinitionContext.directives())
                    .map(this::buildValidation)
                    .orElseGet(jsonProvider::createObjectBuilder)
                    .add("$id", jsonProvider.createValue("#" + inputObjectTypeDefinitionContext.name().getText() + "_update"))
                    .add("type", jsonProvider.createValue("object"))
                    .add("properties", inputObjectToUpdateProperties(inputObjectTypeDefinitionContext))
                    .add("additionalProperties", TRUE)
                    .add("required", buildRequired(inputObjectTypeDefinitionContext));

            String objectName = inputObjectTypeDefinitionContext.name().getText().substring(0, inputObjectTypeDefinitionContext.name().getText().lastIndexOf(INPUT_SUFFIX));
            GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext = manager.getObject(objectName).orElseThrow(() -> new GraphQLErrors(TYPE_NOT_EXIST.bind(objectName)));
            JsonObjectBuilder builder = VALIDATION_UTIL.getValidationDirectiveContext(inputObjectTypeDefinitionContext.directives())
                    .map(this::buildValidation)
                    .orElseGet(jsonProvider::createObjectBuilder)
                    .add("$id", jsonProvider.createValue("#" + inputObjectTypeDefinitionContext.name().getText()))
                    .add("type", jsonProvider.createValue("object"))
                    .add("properties", inputObjectToInsertProperties(inputObjectTypeDefinitionContext, objectTypeDefinitionContext))
                    .add("additionalProperties", TRUE)
                    .add("required", buildRequired(objectTypeDefinitionContext));
            return Stream.of(updateBuilder.build(), builder.build());
        } else if (inputObjectTypeDefinitionContext.name().getText().endsWith("List" + mutationTypeName + ARGUMENTS_SUFFIX)) {
            JsonObjectBuilder updateBuilder = VALIDATION_UTIL.getValidationDirectiveContext(inputObjectTypeDefinitionContext.directives())
                    .map(this::buildValidation)
                    .orElseGet(jsonProvider::createObjectBuilder)
                    .add("$id", jsonProvider.createValue("#" + inputObjectTypeDefinitionContext.name().getText() + "_update"))
                    .add("type", jsonProvider.createValue("object"))
                    .add("properties", inputObjectToUpdateProperties(inputObjectTypeDefinitionContext))
                    .add("additionalProperties", TRUE)
                    .add("required", buildRequired(inputObjectTypeDefinitionContext));

            JsonObjectBuilder builder = VALIDATION_UTIL.getValidationDirectiveContext(inputObjectTypeDefinitionContext.directives())
                    .map(this::buildValidation)
                    .orElseGet(jsonProvider::createObjectBuilder)
                    .add("$id", jsonProvider.createValue("#" + inputObjectTypeDefinitionContext.name().getText()))
                    .add("type", jsonProvider.createValue("object"))
                    .add("properties", inputObjectToListProperties(inputObjectTypeDefinitionContext))
                    .add("additionalProperties", TRUE)
                    .add("required", jsonProvider.createArrayBuilder().add(LIST_INPUT_NAME));
            return Stream.of(updateBuilder.build(), builder.build());
        } else if (inputObjectTypeDefinitionContext.name().getText().endsWith(mutationTypeName + ARGUMENTS_SUFFIX)) {
            JsonObjectBuilder updateBuilder = VALIDATION_UTIL.getValidationDirectiveContext(inputObjectTypeDefinitionContext.directives())
                    .map(this::buildValidation)
                    .orElseGet(jsonProvider::createObjectBuilder)
                    .add("$id", jsonProvider.createValue("#" + inputObjectTypeDefinitionContext.name().getText() + "_update"))
                    .add("type", jsonProvider.createValue("object"))
                    .add("properties", inputObjectToUpdateProperties(inputObjectTypeDefinitionContext))
                    .add("additionalProperties", TRUE)
                    .add("required", buildRequired(inputObjectTypeDefinitionContext));

            String objectName = inputObjectTypeDefinitionContext.name().getText().substring(0, inputObjectTypeDefinitionContext.name().getText().lastIndexOf(mutationTypeName + ARGUMENTS_SUFFIX));
            GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext = manager.getObject(objectName).orElseThrow(() -> new GraphQLErrors(TYPE_NOT_EXIST.bind(objectName)));
            JsonObjectBuilder builder = VALIDATION_UTIL.getValidationDirectiveContext(inputObjectTypeDefinitionContext.directives())
                    .map(this::buildValidation)
                    .orElseGet(jsonProvider::createObjectBuilder)
                    .add("$id", jsonProvider.createValue("#" + inputObjectTypeDefinitionContext.name().getText()))
                    .add("type", jsonProvider.createValue("object"))
                    .add("properties", inputObjectToInsertProperties(inputObjectTypeDefinitionContext, objectTypeDefinitionContext))
                    .add("additionalProperties", TRUE)
                    .add("required", buildRequired(objectTypeDefinitionContext));
            return Stream.of(updateBuilder.build(), builder.build());
        } else if (inputObjectTypeDefinitionContext.name().getText().endsWith(ARGUMENTS_SUFFIX)) {
            JsonObjectBuilder builder = VALIDATION_UTIL.getValidationDirectiveContext(inputObjectTypeDefinitionContext.directives())
                    .map(this::buildValidation)
                    .orElseGet(jsonProvider::createObjectBuilder)
                    .add("$id", jsonProvider.createValue("#" + inputObjectTypeDefinitionContext.name().getText()))
                    .add("type", jsonProvider.createValue("object"))
                    .add("properties", inputObjectToProperties(inputObjectTypeDefinitionContext))
                    .add("additionalProperties", TRUE)
                    .add("required", buildRequired(inputObjectTypeDefinitionContext));
            return Stream.of(builder.build());
        } else {
            JsonObjectBuilder builder = VALIDATION_UTIL.getValidationDirectiveContext(inputObjectTypeDefinitionContext.directives())
                    .map(this::buildValidation)
                    .orElseGet(jsonProvider::createObjectBuilder)
                    .add("$id", jsonProvider.createValue("#" + inputObjectTypeDefinitionContext.name().getText()))
                    .add("type", jsonProvider.createValue("object"))
                    .add("properties", inputObjectToProperties(inputObjectTypeDefinitionContext))
                    .add("additionalProperties", TRUE)
                    .add("required", buildRequired(inputObjectTypeDefinitionContext));
            return Stream.of(builder.build());
        }
    }

    public JsonValue operationObjectToJsonSchema(GraphqlParser.OperationTypeDefinitionContext operationTypeDefinitionContext) {
        String operationTypeName = operationTypeDefinitionContext.typeName().name().getText();
        GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext = manager.getObject(operationTypeName)
                .orElseThrow(() -> new GraphQLErrors(GraphQLErrorType.TYPE_NOT_EXIST.bind(operationTypeDefinitionContext.typeName().name().getText())));
        JsonObjectBuilder jsonSchemaBuilder = jsonProvider.createObjectBuilder();

        JsonObjectBuilder builder = jsonSchemaBuilder.add("$id", jsonProvider.createValue("#" + objectTypeDefinitionContext.name().getText()))
                .add("type", jsonProvider.createValue("object"))
                .add("additionalProperties", TRUE);

        JsonObjectBuilder propertiesBuilder = jsonProvider.createObjectBuilder();
        objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                .filter(fieldDefinitionContext -> fieldDefinitionContext.argumentsDefinition() != null)
                .forEach(fieldDefinitionContext -> {
                            if (manager.isInvokeField(fieldDefinitionContext)) {
                                propertiesBuilder.add(
                                        fieldDefinitionContext.name().getText(),
                                        buildNullableType(
                                                jsonProvider.createObjectBuilder()
                                                        .add("$ref", operationTypeName + "_" + fieldDefinitionContext.name().getText() + "_" + ARGUMENTS_SUFFIX)
                                        )
                                );
                            } else {
                                if (operationTypeDefinitionContext.operationType().MUTATION() != null) {
                                    JsonArrayBuilder jsonArrayBuilder = jsonProvider.createArrayBuilder()
                                            .add(jsonProvider.createObjectBuilder()
                                                    .add("$ref", operationTypeName + "_" + fieldDefinitionContext.name().getText() + "_" + ARGUMENTS_SUFFIX)
                                            )
                                            .add(jsonProvider.createObjectBuilder()
                                                    .add("$ref", operationTypeName + "_" + fieldDefinitionContext.name().getText() + "_" + ARGUMENTS_SUFFIX + "_update")
                                            );
                                    propertiesBuilder.add(
                                            fieldDefinitionContext.name().getText(),
                                            buildNullableType(
                                                    jsonProvider.createObjectBuilder().add("anyOf", jsonArrayBuilder)
                                            )
                                    );
                                } else {
                                    propertiesBuilder.add(
                                            fieldDefinitionContext.name().getText(),
                                            buildNullableType(
                                                    jsonProvider.createObjectBuilder()
                                                            .add("$ref", operationTypeName + "_" + fieldDefinitionContext.name().getText() + "_" + ARGUMENTS_SUFFIX)
                                            )
                                    );
                                }
                            }
                        }
                );
        builder.add("properties", propertiesBuilder);
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

    protected JsonArrayBuilder buildRequired(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        JsonArrayBuilder requiredBuilder = jsonProvider.createArrayBuilder();
        objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                .filter(inputValueDefinitionContext -> inputValueDefinitionContext.type().nonNullType() != null)
                .forEach(inputValueDefinitionContext -> requiredBuilder.add(inputValueDefinitionContext.name().getText()));
        return requiredBuilder;
    }

    protected JsonObjectBuilder inputObjectToProperties(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext) {
        JsonObjectBuilder propertiesBuilder = jsonProvider.createObjectBuilder();
        inputObjectTypeDefinitionContext.inputObjectValueDefinitions().inputValueDefinition()
                .forEach(inputValueDefinitionContext ->
                        propertiesBuilder.add(inputValueDefinitionContext.name().getText(), fieldToProperty(inputValueDefinitionContext.type(), VALIDATION_UTIL.getValidationDirectiveContext(inputValueDefinitionContext.directives()).orElse(null)))
                );
        return propertiesBuilder;
    }

    protected JsonObjectBuilder inputObjectToUpdateProperties(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext) {
        JsonObjectBuilder propertiesBuilder = jsonProvider.createObjectBuilder();
        inputObjectTypeDefinitionContext.inputObjectValueDefinitions().inputValueDefinition()
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
        return propertiesBuilder;
    }

    protected JsonObjectBuilder inputObjectToListProperties(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext) {
        JsonObjectBuilder propertiesBuilder = jsonProvider.createObjectBuilder();
        inputObjectTypeDefinitionContext.inputObjectValueDefinitions().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> inputValueDefinitionContext.name().getText().equals(LIST_INPUT_NAME))
                .forEach(inputValueDefinitionContext ->
                        propertiesBuilder.add(inputValueDefinitionContext.name().getText(), fieldToProperty(inputValueDefinitionContext.type(), VALIDATION_UTIL.getValidationDirectiveContext(inputValueDefinitionContext.directives()).orElse(null)))
                );
        return propertiesBuilder;
    }

    protected JsonObjectBuilder inputObjectToInsertProperties(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext, GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        JsonObjectBuilder propertiesBuilder = jsonProvider.createObjectBuilder();
        inputObjectTypeDefinitionContext.inputObjectValueDefinitions().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> !inputValueDefinitionContext.name().getText().equals(WHERE_INPUT_NAME))
                .forEach(inputValueDefinitionContext -> {
                            Optional<GraphqlParser.FieldDefinitionContext> fieldDefinitionFromInputValueDefinition = manager.getFieldDefinitionFromInputValueDefinition(objectTypeDefinitionContext, inputValueDefinitionContext);
                            if (fieldDefinitionFromInputValueDefinition.isPresent()) {
                                propertiesBuilder.add(fieldDefinitionFromInputValueDefinition.get().name().getText(), fieldToProperty(fieldDefinitionFromInputValueDefinition.get().type(), VALIDATION_UTIL.getValidationDirectiveContext(fieldDefinitionFromInputValueDefinition.get().directives()).orElse(null)));
                            } else {
                                propertiesBuilder.add(inputValueDefinitionContext.name().getText(), fieldToProperty(inputValueDefinitionContext.type(), VALIDATION_UTIL.getValidationDirectiveContext(inputValueDefinitionContext.directives()).orElse(null)));
                            }
                        }
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
            if (fieldTypeName.endsWith(INPUT_SUFFIX)) {
                jsonObjectBuilder
                        .add("anyOf",
                                jsonProvider.createArrayBuilder()
                                        .add(jsonProvider.createObjectBuilder().add("$ref", jsonProvider.createValue(fieldTypeName)))
                                        .add(jsonProvider.createObjectBuilder().add("$ref", jsonProvider.createValue(fieldTypeName + "_update")))
                        );
            } else {
                jsonObjectBuilder.add("$ref", jsonProvider.createValue(fieldTypeName));
            }
        } else if (manager.isObject(fieldTypeName)) {
            jsonObjectBuilder
                    .add("anyOf",
                            jsonProvider.createArrayBuilder()
                                    .add(jsonProvider.createObjectBuilder().add("$ref", jsonProvider.createValue(fieldTypeName + INPUT_SUFFIX)))
                                    .add(jsonProvider.createObjectBuilder().add("$ref", jsonProvider.createValue(fieldTypeName + INPUT_SUFFIX + "_update")))
                    );
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
