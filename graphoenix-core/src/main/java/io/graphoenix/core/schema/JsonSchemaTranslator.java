package io.graphoenix.core.schema;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.*;
import jakarta.json.spi.JsonProvider;

import java.io.StringWriter;
import java.util.Optional;

import static io.graphoenix.core.utils.DocumentUtil.DOCUMENT_UTIL;
import static io.graphoenix.spi.constant.Hammurabi.AGGREGATE_SUFFIX;
import static jakarta.json.JsonValue.TRUE;

@ApplicationScoped
public class JsonSchemaTranslator {

    private final JsonProvider jsonProvider;
    private final IGraphQLDocumentManager manager;

    @Inject
    public JsonSchemaTranslator(IGraphQLDocumentManager manager,
                                JsonProvider jsonProvider) {
        this.manager = manager;
        this.jsonProvider = jsonProvider;
    }

    public String objectToJsonSchemaString(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        return objectToJsonSchemaString(objectTypeDefinitionContext, false);
    }

    public String objectToJsonSchemaString(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext, boolean isUpdate) {
        StringWriter stringWriter = new StringWriter();
        jsonProvider.createWriter(stringWriter).write(objectToJsonSchema(objectTypeDefinitionContext, isUpdate));
        return stringWriter.toString();
    }

    public String objectListToJsonSchemaString(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        StringWriter stringWriter = new StringWriter();
        jsonProvider.createWriter(stringWriter).write(objectListToJsonSchema(objectTypeDefinitionContext));
        return stringWriter.toString();
    }

    public JsonValue objectToJsonSchema(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext, boolean isUpdate) {
        JsonObjectBuilder jsonSchemaBuilder = getValidationDirectiveContext(objectTypeDefinitionContext.directives())
                .map(this::buildValidation)
                .orElseGet(jsonProvider::createObjectBuilder);
        JsonObjectBuilder builder = jsonSchemaBuilder.add("$id", jsonProvider.createValue("#".concat(isUpdate ? objectTypeDefinitionContext.name().getText().concat("Update") : objectTypeDefinitionContext.name().getText())))
                .add("type", jsonProvider.createValue("object"))
                .add("properties", objectToProperties(objectTypeDefinitionContext, isUpdate))
                .add("additionalProperties", TRUE);
        if (!isUpdate) {
            builder.add("required", buildRequired(objectTypeDefinitionContext));
        }
        return builder.build();
    }

    public JsonValue objectListToJsonSchema(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        JsonObjectBuilder jsonSchemaBuilder = getValidationDirectiveContext(objectTypeDefinitionContext.directives())
                .map(this::buildValidation)
                .orElseGet(jsonProvider::createObjectBuilder);
        return jsonSchemaBuilder.add("$id", jsonProvider.createValue("#".concat(objectTypeDefinitionContext.name().getText().concat("List"))))
                .add("type", jsonProvider.createValue("object"))
                .add("properties",
                        jsonProvider.createObjectBuilder()
                                .add("list",
                                        jsonProvider.createObjectBuilder()
                                                .add("type", jsonProvider.createValue("array"))
                                                .add("items", jsonProvider.createObjectBuilder().add("$ref", objectTypeDefinitionContext.name().getText()))
                                )
                )
                .build();
    }

    protected JsonArrayBuilder buildRequired(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        JsonArrayBuilder requiredBuilder = jsonProvider.createArrayBuilder();
        objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                .filter(fieldDefinitionContext -> fieldDefinitionContext.type().nonNullType() != null)
                .forEach(fieldDefinitionContext -> requiredBuilder.add(fieldDefinitionContext.name().getText()));
        return requiredBuilder;
    }

    protected JsonObjectBuilder objectToProperties(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext, boolean isUpdate) {
        JsonObjectBuilder propertiesBuilder = jsonProvider.createObjectBuilder();
        objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                .filter(fieldDefinitionContext -> manager.isNotConnectionField(objectTypeDefinitionContext.name().getText(), fieldDefinitionContext.name().getText()))
                .filter(fieldDefinitionContext -> manager.isNotFunctionField(objectTypeDefinitionContext.name().getText(), fieldDefinitionContext.name().getText()))
                .filter(fieldDefinitionContext -> !fieldDefinitionContext.name().getText().endsWith(AGGREGATE_SUFFIX))
                .forEach(fieldDefinitionContext ->
                        propertiesBuilder.add(fieldDefinitionContext.name().getText(), fieldToProperty(fieldDefinitionContext, isUpdate))
                );
        return propertiesBuilder;
    }

    protected JsonObjectBuilder fieldToProperty(GraphqlParser.FieldDefinitionContext fieldDefinitionContext, boolean isUpdate) {
        JsonObjectBuilder propertyBuilder = getValidationDirectiveContext(fieldDefinitionContext.directives())
                .map(this::buildValidation)
                .orElseGet(jsonProvider::createObjectBuilder);
        if (manager.fieldTypeIsList(fieldDefinitionContext.type())) {
            propertyBuilder.add("type", jsonProvider.createValue("array"));
            JsonObjectBuilder itemsBuilder = getValidationDirectiveContext(fieldDefinitionContext.directives())
                    .flatMap(directiveContext -> getValidationObjectArgument(directiveContext, "items"))
                    .map(this::buildValidation)
                    .orElseGet(jsonProvider::createObjectBuilder);
            propertyBuilder.add("items", buildType(manager.getFieldTypeName(fieldDefinitionContext.type()), fieldDefinitionContext.type().nonNullType() != null, itemsBuilder, isUpdate));
            return propertyBuilder;
        } else {
            return buildType(manager.getFieldTypeName(fieldDefinitionContext.type()), fieldDefinitionContext.type().nonNullType() != null, propertyBuilder, isUpdate);
        }
    }

    protected JsonObjectBuilder buildType(String fieldTypeName, boolean isNonNull, JsonObjectBuilder jsonObjectBuilder, boolean isUpdate) {
        if (manager.isScalar(fieldTypeName)) {
            switch (fieldTypeName) {
                case "ID":
                case "String":
                case "Date":
                case "Time":
                case "DateTime":
                case "Timestamp":
                    if (isNonNull) {
                        jsonObjectBuilder.add("type", jsonProvider.createValue("string"));
                    } else {
                        jsonObjectBuilder.add("type", jsonProvider.createArrayBuilder().add(jsonProvider.createValue("string")).add(jsonProvider.createValue("null")));
                    }
                    break;
                case "Boolean":
                    if (isNonNull) {
                        jsonObjectBuilder.add("type", jsonProvider.createValue("boolean"));
                    } else {
                        jsonObjectBuilder.add("type", jsonProvider.createArrayBuilder().add(jsonProvider.createValue("boolean")).add(jsonProvider.createValue("null")));
                    }
                    break;
                case "Int":
                case "BigInteger":
                    if (isNonNull) {
                        jsonObjectBuilder.add("type", jsonProvider.createValue("integer"));
                    } else {
                        jsonObjectBuilder.add("type", jsonProvider.createArrayBuilder().add(jsonProvider.createValue("integer")).add(jsonProvider.createValue("null")));
                    }
                    break;
                case "Float":
                case "BigDecimal":
                    if (isNonNull) {
                        jsonObjectBuilder.add("type", jsonProvider.createValue("number"));
                    } else {
                        jsonObjectBuilder.add("type", jsonProvider.createArrayBuilder().add(jsonProvider.createValue("number")).add(jsonProvider.createValue("null")));
                    }
                    break;
            }
        } else if (manager.isEnum(fieldTypeName)) {
            JsonArrayBuilder enumValuesBuilder = jsonProvider.createArrayBuilder();
            manager.getEnum(fieldTypeName)
                    .ifPresent(enumTypeDefinitionContext -> {
                                enumTypeDefinitionContext.enumValueDefinitions().enumValueDefinition()
                                        .forEach(enumValueDefinitionContext -> enumValuesBuilder.add(enumValueDefinitionContext.enumValue().getText()));
                                if (!isNonNull) {
                                    enumValuesBuilder.add(jsonProvider.createValue("null"));
                                }
                                jsonObjectBuilder.add("enum", enumValuesBuilder);
                            }
                    );
        } else if (manager.isObject(fieldTypeName)) {
            String $ref = isUpdate ? fieldTypeName.concat("Update") : fieldTypeName;
            if (isNonNull) {
                jsonObjectBuilder.add("$ref", jsonProvider.createValue($ref));
            } else {
                jsonObjectBuilder.add("oneOf",
                        jsonProvider.createArrayBuilder()
                                .add(jsonProvider.createObjectBuilder().add("type", jsonProvider.createValue("null")))
                                .add(jsonProvider.createObjectBuilder().add("$ref", jsonProvider.createValue($ref)))
                );
            }
        }
        return jsonObjectBuilder;
    }

    protected JsonObjectBuilder buildValidation(GraphqlParser.DirectiveContext directiveContext) {
        JsonObjectBuilder validationBuilder = jsonProvider.createObjectBuilder();

        getValidationIntArgument(directiveContext, "minLength")
                .ifPresent(minLength -> validationBuilder.add("minLength", minLength));
        getValidationIntArgument(directiveContext, "maxLength")
                .ifPresent(maxLength -> validationBuilder.add("maxLength", maxLength));
        getValidationStringArgument(directiveContext, "pattern")
                .ifPresent(pattern -> validationBuilder.add("pattern", pattern));
        getValidationStringArgument(directiveContext, "format")
                .ifPresent(format -> validationBuilder.add("format", format));
        getValidationStringArgument(directiveContext, "contentMediaType")
                .ifPresent(contentMediaType -> validationBuilder.add("contentMediaType", contentMediaType));
        getValidationStringArgument(directiveContext, "contentEncoding")
                .ifPresent(contentEncoding -> validationBuilder.add("contentEncoding", contentEncoding));

        getValidationFloatArgument(directiveContext, "minimum")
                .ifPresent(minimum -> validationBuilder.add("minimum", minimum));
        getValidationFloatArgument(directiveContext, "exclusiveMinimum")
                .ifPresent(exclusiveMinimum -> validationBuilder.add("exclusiveMinimum", exclusiveMinimum));
        getValidationFloatArgument(directiveContext, "maximum")
                .ifPresent(maximum -> validationBuilder.add("maximum", maximum));
        getValidationFloatArgument(directiveContext, "exclusiveMaximum")
                .ifPresent(exclusiveMaximum -> validationBuilder.add("exclusiveMaximum", exclusiveMaximum));
        getValidationFloatArgument(directiveContext, "multipleOf")
                .ifPresent(multipleOf -> validationBuilder.add("multipleOf", multipleOf));

        getValidationStringArgument(directiveContext, "const")
                .ifPresent(constValue -> validationBuilder.add("const", constValue));

        getValidationArrayArgument(directiveContext, "allOf")
                .ifPresent(arrayValueWithVariableContext -> {
                            JsonArrayBuilder allOfBuilder = jsonProvider.createArrayBuilder();
                            arrayValueWithVariableContext.valueWithVariable().stream()
                                    .filter(valueWithVariableContext -> valueWithVariableContext.objectValueWithVariable() != null)
                                    .forEach(valueWithVariableContext -> allOfBuilder.add(buildValidation(valueWithVariableContext.objectValueWithVariable())));
                            validationBuilder.add("allOf", allOfBuilder);
                        }
                );

        getValidationArrayArgument(directiveContext, "anyOf")
                .ifPresent(arrayValueWithVariableContext -> {
                            JsonArrayBuilder anyOfBuilder = jsonProvider.createArrayBuilder();
                            arrayValueWithVariableContext.valueWithVariable().stream()
                                    .filter(valueWithVariableContext -> valueWithVariableContext.objectValueWithVariable() != null)
                                    .forEach(valueWithVariableContext -> anyOfBuilder.add(buildValidation(valueWithVariableContext.objectValueWithVariable())));
                            validationBuilder.add("anyOf", anyOfBuilder);
                        }
                );

        getValidationArrayArgument(directiveContext, "oneOf")
                .ifPresent(arrayValueWithVariableContext -> {
                            JsonArrayBuilder oneOfBuilder = jsonProvider.createArrayBuilder();
                            arrayValueWithVariableContext.valueWithVariable().stream()
                                    .filter(valueWithVariableContext -> valueWithVariableContext.objectValueWithVariable() != null)
                                    .forEach(valueWithVariableContext -> oneOfBuilder.add(buildValidation(valueWithVariableContext.objectValueWithVariable())));
                            validationBuilder.add("oneOf", oneOfBuilder);
                        }
                );

        getValidationObjectArgument(directiveContext, "not")
                .ifPresent(not -> validationBuilder.add("not", buildValidation(not)));

        getValidationArrayArgument(directiveContext, "properties")
                .ifPresent(arrayValueWithVariableContext -> validationBuilder.add("properties", buildProperties(arrayValueWithVariableContext)));

        getValidationObjectArgument(directiveContext, "if")
                .ifPresent(ifValidation -> validationBuilder.add("if", buildValidation(ifValidation)));

        getValidationObjectArgument(directiveContext, "then")
                .ifPresent(thenValidation -> validationBuilder.add("then", buildValidation(thenValidation)));

        getValidationObjectArgument(directiveContext, "else")
                .ifPresent(elseValidation -> validationBuilder.add("else", buildValidation(elseValidation)));

        getValidationArrayArgument(directiveContext, "dependentRequired")
                .ifPresent(arrayValueWithVariableContext -> validationBuilder.add("dependentRequired", buildDependentRequired(arrayValueWithVariableContext)));

        return validationBuilder;
    }

    protected JsonObjectBuilder buildValidation(GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {
        JsonObjectBuilder validationBuilder = jsonProvider.createObjectBuilder();

        getValidationIntArgument(objectValueWithVariableContext, "minLength")
                .ifPresent(minLength -> validationBuilder.add("minLength", minLength));
        getValidationIntArgument(objectValueWithVariableContext, "maxLength")
                .ifPresent(maxLength -> validationBuilder.add("maxLength", maxLength));
        getValidationStringArgument(objectValueWithVariableContext, "pattern")
                .ifPresent(pattern -> validationBuilder.add("pattern", pattern));
        getValidationStringArgument(objectValueWithVariableContext, "format")
                .ifPresent(format -> validationBuilder.add("format", format));
        getValidationStringArgument(objectValueWithVariableContext, "contentMediaType")
                .ifPresent(contentMediaType -> validationBuilder.add("contentMediaType", contentMediaType));
        getValidationStringArgument(objectValueWithVariableContext, "contentEncoding")
                .ifPresent(contentEncoding -> validationBuilder.add("contentEncoding", contentEncoding));

        getValidationFloatArgument(objectValueWithVariableContext, "minimum")
                .ifPresent(minimum -> validationBuilder.add("minimum", minimum));
        getValidationFloatArgument(objectValueWithVariableContext, "exclusiveMinimum")
                .ifPresent(exclusiveMinimum -> validationBuilder.add("exclusiveMinimum", exclusiveMinimum));
        getValidationFloatArgument(objectValueWithVariableContext, "maximum")
                .ifPresent(maximum -> validationBuilder.add("maximum", maximum));
        getValidationFloatArgument(objectValueWithVariableContext, "exclusiveMaximum")
                .ifPresent(exclusiveMaximum -> validationBuilder.add("exclusiveMaximum", exclusiveMaximum));
        getValidationFloatArgument(objectValueWithVariableContext, "multipleOf")
                .ifPresent(multipleOf -> validationBuilder.add("multipleOf", multipleOf));

        getValidationStringArgument(objectValueWithVariableContext, "const")
                .ifPresent(constValue -> validationBuilder.add("const", constValue));

        getValidationArrayArgument(objectValueWithVariableContext, "allOf")
                .ifPresent(arrayValueWithVariableContext -> {
                            JsonArrayBuilder allOfBuilder = jsonProvider.createArrayBuilder();
                            arrayValueWithVariableContext.valueWithVariable().stream()
                                    .filter(valueWithVariableContext -> valueWithVariableContext.objectValueWithVariable() != null)
                                    .forEach(valueWithVariableContext -> allOfBuilder.add(buildValidation(valueWithVariableContext.objectValueWithVariable())));
                            validationBuilder.add("allOf", allOfBuilder);
                        }
                );

        getValidationArrayArgument(objectValueWithVariableContext, "anyOf")
                .ifPresent(arrayValueWithVariableContext -> {
                            JsonArrayBuilder anyOfBuilder = jsonProvider.createArrayBuilder();
                            arrayValueWithVariableContext.valueWithVariable().stream()
                                    .filter(valueWithVariableContext -> valueWithVariableContext.objectValueWithVariable() != null)
                                    .forEach(valueWithVariableContext -> anyOfBuilder.add(buildValidation(valueWithVariableContext.objectValueWithVariable())));
                            validationBuilder.add("anyOf", anyOfBuilder);
                        }
                );

        getValidationArrayArgument(objectValueWithVariableContext, "oneOf")
                .ifPresent(arrayValueWithVariableContext -> {
                            JsonArrayBuilder oneOfBuilder = jsonProvider.createArrayBuilder();
                            arrayValueWithVariableContext.valueWithVariable().stream()
                                    .filter(valueWithVariableContext -> valueWithVariableContext.objectValueWithVariable() != null)
                                    .forEach(valueWithVariableContext -> oneOfBuilder.add(buildValidation(valueWithVariableContext.objectValueWithVariable())));
                            validationBuilder.add("oneOf", oneOfBuilder);
                        }
                );

        getValidationObjectArgument(objectValueWithVariableContext, "not")
                .ifPresent(not -> validationBuilder.add("not", buildValidation(not)));

        getValidationArrayArgument(objectValueWithVariableContext, "properties")
                .ifPresent(arrayValueWithVariableContext -> validationBuilder.add("properties", buildProperties(arrayValueWithVariableContext)));

        getValidationObjectArgument(objectValueWithVariableContext, "if")
                .ifPresent(ifValidation -> validationBuilder.add("if", buildValidation(ifValidation)));

        getValidationObjectArgument(objectValueWithVariableContext, "then")
                .ifPresent(thenValidation -> validationBuilder.add("then", buildValidation(thenValidation)));

        getValidationObjectArgument(objectValueWithVariableContext, "else")
                .ifPresent(elseValidation -> validationBuilder.add("else", buildValidation(elseValidation)));

        getValidationArrayArgument(objectValueWithVariableContext, "dependentRequired")
                .ifPresent(arrayValueWithVariableContext -> validationBuilder.add("dependentRequired", buildDependentRequired(arrayValueWithVariableContext)));

        return validationBuilder;
    }

    protected JsonObjectBuilder buildProperties(GraphqlParser.ArrayValueWithVariableContext arrayValueWithVariableContext) {
        JsonObjectBuilder propertiesBuilder = jsonProvider.createObjectBuilder();
        arrayValueWithVariableContext.valueWithVariable().stream()
                .filter(property -> property.objectValueWithVariable() != null)
                .forEach(property ->
                        getValidationStringArgument(property.objectValueWithVariable(), "name")
                                .ifPresent(name ->
                                        getValidationObjectArgument(property.objectValueWithVariable(), "validation")
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
                        getValidationStringArgument(property.objectValueWithVariable(), "name")
                                .ifPresent(name ->
                                        getValidationArrayArgument(property.objectValueWithVariable(), "required")
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

    protected Optional<GraphqlParser.DirectiveContext> getValidationDirectiveContext(GraphqlParser.DirectivesContext directivesContext) {
        if (directivesContext == null) {
            return Optional.empty();
        }
        return directivesContext.directive().stream()
                .filter(directiveContext -> directiveContext.name().getText().equals("validation"))
                .findFirst();
    }

    protected Optional<String> getValidationStringArgument(GraphqlParser.DirectiveContext directiveContext, String argumentName) {
        return directiveContext.arguments().argument().stream()
                .filter(argumentContext -> argumentContext.valueWithVariable().StringValue() != null)
                .filter(argumentContext -> argumentContext.name().getText().equals(argumentName))
                .findFirst()
                .map(argumentContext -> DOCUMENT_UTIL.getStringValue(argumentContext.valueWithVariable().StringValue()));
    }

    protected Optional<Float> getValidationFloatArgument(GraphqlParser.DirectiveContext directiveContext, String argumentName) {
        return directiveContext.arguments().argument().stream()
                .filter(argumentContext -> argumentContext.valueWithVariable().FloatValue() != null)
                .filter(argumentContext -> argumentContext.name().getText().equals(argumentName))
                .findFirst()
                .map(argumentContext -> Float.parseFloat(argumentContext.valueWithVariable().FloatValue().getText()));
    }

    protected Optional<Integer> getValidationIntArgument(GraphqlParser.DirectiveContext directiveContext, String argumentName) {
        return directiveContext.arguments().argument().stream()
                .filter(argumentContext -> argumentContext.valueWithVariable().IntValue() != null)
                .filter(argumentContext -> argumentContext.name().getText().equals(argumentName))
                .findFirst()
                .map(argumentContext -> Integer.parseInt(argumentContext.valueWithVariable().IntValue().getText()));
    }

    protected Optional<Boolean> getValidationBooleanArgument(GraphqlParser.DirectiveContext directiveContext, String argumentName) {
        return directiveContext.arguments().argument().stream()
                .filter(argumentContext -> argumentContext.valueWithVariable().BooleanValue() != null)
                .filter(argumentContext -> argumentContext.name().getText().equals(argumentName))
                .findFirst()
                .map(argumentContext -> Boolean.parseBoolean(argumentContext.valueWithVariable().BooleanValue().getText()));
    }

    protected Optional<GraphqlParser.ObjectValueWithVariableContext> getValidationObjectArgument(GraphqlParser.DirectiveContext directiveContext, String argumentName) {
        return directiveContext.arguments().argument().stream()
                .filter(argumentContext -> argumentContext.valueWithVariable().objectValueWithVariable() != null)
                .filter(argumentContext -> argumentContext.name().getText().equals(argumentName))
                .findFirst()
                .map(argumentContext -> argumentContext.valueWithVariable().objectValueWithVariable());
    }

    protected Optional<GraphqlParser.ArrayValueWithVariableContext> getValidationArrayArgument(GraphqlParser.DirectiveContext directiveContext, String argumentName) {
        return directiveContext.arguments().argument().stream()
                .filter(argumentContext -> argumentContext.valueWithVariable().arrayValueWithVariable() != null)
                .filter(argumentContext -> argumentContext.name().getText().equals(argumentName))
                .findFirst()
                .map(argumentContext -> argumentContext.valueWithVariable().arrayValueWithVariable());
    }

    protected Optional<String> getValidationStringArgument(GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext, String argumentName) {
        return objectValueWithVariableContext.objectFieldWithVariable().stream()
                .filter(objectFieldWithVariableContext -> objectFieldWithVariableContext.name().getText().equals(argumentName))
                .filter(objectFieldWithVariableContext -> objectFieldWithVariableContext.valueWithVariable().StringValue() != null)
                .findFirst()
                .map(argumentContext -> DOCUMENT_UTIL.getStringValue(argumentContext.valueWithVariable().StringValue()));
    }

    protected Optional<Float> getValidationFloatArgument(GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext, String argumentName) {
        return objectValueWithVariableContext.objectFieldWithVariable().stream()
                .filter(objectFieldWithVariableContext -> objectFieldWithVariableContext.name().getText().equals(argumentName))
                .filter(objectFieldWithVariableContext -> objectFieldWithVariableContext.valueWithVariable().FloatValue() != null)
                .findFirst()
                .map(argumentContext -> Float.parseFloat(argumentContext.valueWithVariable().FloatValue().getText()));
    }

    protected Optional<Integer> getValidationIntArgument(GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext, String argumentName) {
        return objectValueWithVariableContext.objectFieldWithVariable().stream()
                .filter(objectFieldWithVariableContext -> objectFieldWithVariableContext.name().getText().equals(argumentName))
                .filter(objectFieldWithVariableContext -> objectFieldWithVariableContext.valueWithVariable().IntValue() != null)
                .findFirst()
                .map(argumentContext -> Integer.parseInt(argumentContext.valueWithVariable().IntValue().getText()));
    }

    protected Optional<Boolean> getValidationBooleanArgument(GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext, String argumentName) {
        return objectValueWithVariableContext.objectFieldWithVariable().stream()
                .filter(objectFieldWithVariableContext -> objectFieldWithVariableContext.name().getText().equals(argumentName))
                .filter(objectFieldWithVariableContext -> objectFieldWithVariableContext.valueWithVariable().BooleanValue() != null)
                .findFirst()
                .map(argumentContext -> Boolean.parseBoolean(argumentContext.valueWithVariable().BooleanValue().getText()));
    }

    protected Optional<GraphqlParser.ObjectValueWithVariableContext> getValidationObjectArgument(GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext, String argumentName) {
        return objectValueWithVariableContext.objectFieldWithVariable().stream()
                .filter(objectFieldWithVariableContext -> objectFieldWithVariableContext.name().getText().equals(argumentName))
                .filter(objectFieldWithVariableContext -> objectFieldWithVariableContext.valueWithVariable().BooleanValue() != null)
                .findFirst()
                .map(objectFieldWithVariableContext -> objectFieldWithVariableContext.valueWithVariable().objectValueWithVariable());
    }

    protected Optional<GraphqlParser.ArrayValueWithVariableContext> getValidationArrayArgument(GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext, String argumentName) {
        return objectValueWithVariableContext.objectFieldWithVariable().stream()
                .filter(objectFieldWithVariableContext -> objectFieldWithVariableContext.name().getText().equals(argumentName))
                .filter(objectFieldWithVariableContext -> objectFieldWithVariableContext.valueWithVariable().BooleanValue() != null)
                .findFirst()
                .map(objectFieldWithVariableContext -> objectFieldWithVariableContext.valueWithVariable().arrayValueWithVariable());
    }
}
