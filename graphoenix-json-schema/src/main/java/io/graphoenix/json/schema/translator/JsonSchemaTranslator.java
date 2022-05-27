package io.graphoenix.json.schema.translator;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Optional;

import static io.graphoenix.core.utils.DocumentUtil.DOCUMENT_UTIL;

@ApplicationScoped
public class JsonSchemaTranslator {

    private final GraphQLConfig graphQLConfig;
    private final GsonBuilder gsonBuilder;
    private final IGraphQLDocumentManager manager;

    @Inject
    public JsonSchemaTranslator(GraphQLConfig graphQLConfig,
                                IGraphQLDocumentManager manager) {
        this.graphQLConfig = graphQLConfig;
        this.manager = manager;
        this.gsonBuilder = new GsonBuilder().setPrettyPrinting();
    }

    public String objectToJsonSchemaString(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        return this.gsonBuilder.create().toJson(objectToJsonSchema(objectTypeDefinitionContext));
    }

    public JsonObject objectToJsonSchema(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        JsonObject jsonSchema = getValidationDirectiveContext(objectTypeDefinitionContext.directives())
                .map(this::buildValidation)
                .orElseGet(JsonObject::new);
        jsonSchema.addProperty("$id", "/schema/".concat(objectTypeDefinitionContext.name().getText()));
        jsonSchema.addProperty("type", "object");
        jsonSchema.add("properties", objectToProperties(objectTypeDefinitionContext));
        jsonSchema.add("required", buildRequired(objectTypeDefinitionContext));
        jsonSchema.addProperty("additionalProperties", false);
        return jsonSchema;
    }

    protected JsonArray buildRequired(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        JsonArray required = new JsonArray();
        objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                .filter(fieldDefinitionContext -> fieldDefinitionContext.type().nonNullType() != null)
                .forEach(fieldDefinitionContext -> required.add(fieldDefinitionContext.name().getText()));
        return required;
    }

    protected JsonObject objectToProperties(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        JsonObject properties = new JsonObject();
        objectTypeDefinitionContext.fieldsDefinition().fieldDefinition()
                .forEach(fieldDefinitionContext ->
                        properties.add(fieldDefinitionContext.name().getText(), fieldToProperty(fieldDefinitionContext))
                );
        return properties;
    }

    protected JsonObject fieldToProperty(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        JsonObject property = getValidationDirectiveContext(fieldDefinitionContext.directives())
                .map(this::buildValidation)
                .orElseGet(JsonObject::new);
        if (manager.fieldTypeIsList(fieldDefinitionContext.type())) {
            property.addProperty("type", "array");
            JsonObject items = getValidationDirectiveContext(fieldDefinitionContext.directives())
                    .flatMap(directiveContext -> getValidationObjectArgument(directiveContext, "items"))
                    .map(this::buildValidation)
                    .orElseGet(JsonObject::new);
            property.add("items", buildType(manager.getFieldTypeName(fieldDefinitionContext.type()), items));
            return property;
        } else {
            return buildType(manager.getFieldTypeName(fieldDefinitionContext.type()), property);
        }
    }

    protected JsonObject buildType(String fieldTypeName, JsonObject jsonObject) {
        if (manager.isScalar(fieldTypeName)) {
            switch (fieldTypeName) {
                case "ID":
                case "String":
                case "Date":
                case "Time":
                case "DateTime":
                case "Timestamp":
                    jsonObject.addProperty("type", "string");
                    break;
                case "Boolean":
                    jsonObject.addProperty("type", "boolean");
                    break;
                case "Int":
                case "BigInteger":
                case "Float":
                case "BigDecimal":
                    jsonObject.addProperty("type", "number");
                    break;
            }
        } else if (manager.isEnum(fieldTypeName)) {
            jsonObject.addProperty("type", "string");
            JsonArray enumValues = new JsonArray();
            manager.getEnum(fieldTypeName)
                    .ifPresent(enumTypeDefinitionContext -> {
                                enumTypeDefinitionContext.enumValueDefinitions().enumValueDefinition()
                                        .forEach(enumValueDefinitionContext -> enumValues.add(enumValueDefinitionContext.enumValue().getText()));
                                jsonObject.add("enum", enumValues);
                            }
                    );
        }
        return jsonObject;
    }

    protected JsonObject buildValidation(GraphqlParser.DirectiveContext directiveContext) {
        JsonObject validation = new JsonObject();

        getValidationIntArgument(directiveContext, "minLength")
                .ifPresent(minLength -> validation.addProperty("minLength", minLength));
        getValidationIntArgument(directiveContext, "maxLength")
                .ifPresent(maxLength -> validation.addProperty("maxLength", maxLength));
        getValidationStringArgument(directiveContext, "pattern")
                .ifPresent(pattern -> validation.addProperty("pattern", pattern));
        getValidationStringArgument(directiveContext, "format")
                .ifPresent(format -> validation.addProperty("format", format));
        getValidationStringArgument(directiveContext, "contentMediaType")
                .ifPresent(contentMediaType -> validation.addProperty("contentMediaType", contentMediaType));
        getValidationStringArgument(directiveContext, "contentEncoding")
                .ifPresent(contentEncoding -> validation.addProperty("contentEncoding", contentEncoding));

        getValidationFloatArgument(directiveContext, "minimum")
                .ifPresent(minimum -> validation.addProperty("minimum", minimum));
        getValidationFloatArgument(directiveContext, "exclusiveMinimum")
                .ifPresent(exclusiveMinimum -> validation.addProperty("exclusiveMinimum", exclusiveMinimum));
        getValidationFloatArgument(directiveContext, "maximum")
                .ifPresent(maximum -> validation.addProperty("maximum", maximum));
        getValidationFloatArgument(directiveContext, "exclusiveMaximum")
                .ifPresent(exclusiveMaximum -> validation.addProperty("exclusiveMaximum", exclusiveMaximum));
        getValidationFloatArgument(directiveContext, "multipleOf")
                .ifPresent(multipleOf -> validation.addProperty("multipleOf", multipleOf));

        getValidationStringArgument(directiveContext, "const")
                .ifPresent(constValue -> validation.addProperty("const", constValue));

        getValidationArrayArgument(directiveContext, "allOf")
                .ifPresent(arrayValueWithVariableContext -> {
                            JsonArray allOf = new JsonArray();
                            arrayValueWithVariableContext.valueWithVariable().stream()
                                    .filter(valueWithVariableContext -> valueWithVariableContext.objectValueWithVariable() != null)
                                    .forEach(valueWithVariableContext -> allOf.add(buildValidation(valueWithVariableContext.objectValueWithVariable())));
                            validation.add("allOf", allOf);
                        }
                );

        getValidationArrayArgument(directiveContext, "anyOf")
                .ifPresent(arrayValueWithVariableContext -> {
                            JsonArray anyOf = new JsonArray();
                            arrayValueWithVariableContext.valueWithVariable().stream()
                                    .filter(valueWithVariableContext -> valueWithVariableContext.objectValueWithVariable() != null)
                                    .forEach(valueWithVariableContext -> anyOf.add(buildValidation(valueWithVariableContext.objectValueWithVariable())));
                            validation.add("anyOf", anyOf);
                        }
                );

        getValidationArrayArgument(directiveContext, "oneOf")
                .ifPresent(arrayValueWithVariableContext -> {
                            JsonArray oneOf = new JsonArray();
                            arrayValueWithVariableContext.valueWithVariable().stream()
                                    .filter(valueWithVariableContext -> valueWithVariableContext.objectValueWithVariable() != null)
                                    .forEach(valueWithVariableContext -> oneOf.add(buildValidation(valueWithVariableContext.objectValueWithVariable())));
                            validation.add("oneOf", oneOf);
                        }
                );

        getValidationObjectArgument(directiveContext, "not")
                .ifPresent(not -> validation.add("not", buildValidation(not)));

        getValidationArrayArgument(directiveContext, "properties")
                .ifPresent(arrayValueWithVariableContext -> validation.add("properties", buildProperties(arrayValueWithVariableContext)));

        getValidationArrayArgument(directiveContext, "if")
                .ifPresent(arrayValueWithVariableContext -> validation.add("if", buildProperties(arrayValueWithVariableContext)));

        getValidationArrayArgument(directiveContext, "then")
                .ifPresent(arrayValueWithVariableContext -> validation.add("then", buildProperties(arrayValueWithVariableContext)));

        getValidationArrayArgument(directiveContext, "else")
                .ifPresent(arrayValueWithVariableContext -> validation.add("else", buildProperties(arrayValueWithVariableContext)));

        getValidationArrayArgument(directiveContext, "dependentRequired")
                .ifPresent(arrayValueWithVariableContext -> validation.add("dependentRequired", buildDependentRequired(arrayValueWithVariableContext)));

        return validation;
    }

    protected JsonObject buildValidation(GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {
        JsonObject validation = new JsonObject();

        getValidationIntArgument(objectValueWithVariableContext, "minLength")
                .ifPresent(minLength -> validation.addProperty("minLength", minLength));
        getValidationIntArgument(objectValueWithVariableContext, "maxLength")
                .ifPresent(maxLength -> validation.addProperty("maxLength", maxLength));
        getValidationStringArgument(objectValueWithVariableContext, "pattern")
                .ifPresent(pattern -> validation.addProperty("pattern", pattern));
        getValidationStringArgument(objectValueWithVariableContext, "format")
                .ifPresent(format -> validation.addProperty("format", format));
        getValidationStringArgument(objectValueWithVariableContext, "contentMediaType")
                .ifPresent(contentMediaType -> validation.addProperty("contentMediaType", contentMediaType));
        getValidationStringArgument(objectValueWithVariableContext, "contentEncoding")
                .ifPresent(contentEncoding -> validation.addProperty("contentEncoding", contentEncoding));

        getValidationFloatArgument(objectValueWithVariableContext, "minimum")
                .ifPresent(minimum -> validation.addProperty("minimum", minimum));
        getValidationFloatArgument(objectValueWithVariableContext, "exclusiveMinimum")
                .ifPresent(exclusiveMinimum -> validation.addProperty("exclusiveMinimum", exclusiveMinimum));
        getValidationFloatArgument(objectValueWithVariableContext, "maximum")
                .ifPresent(maximum -> validation.addProperty("maximum", maximum));
        getValidationFloatArgument(objectValueWithVariableContext, "exclusiveMaximum")
                .ifPresent(exclusiveMaximum -> validation.addProperty("exclusiveMaximum", exclusiveMaximum));
        getValidationFloatArgument(objectValueWithVariableContext, "multipleOf")
                .ifPresent(multipleOf -> validation.addProperty("multipleOf", multipleOf));

        getValidationStringArgument(objectValueWithVariableContext, "const")
                .ifPresent(constValue -> validation.addProperty("const", constValue));

        getValidationArrayArgument(objectValueWithVariableContext, "allOf")
                .ifPresent(arrayValueWithVariableContext -> {
                            JsonArray allOf = new JsonArray();
                            arrayValueWithVariableContext.valueWithVariable().stream()
                                    .filter(valueWithVariableContext -> valueWithVariableContext.objectValueWithVariable() != null)
                                    .forEach(valueWithVariableContext -> allOf.add(buildValidation(valueWithVariableContext.objectValueWithVariable())));
                            validation.add("allOf", allOf);
                        }
                );

        getValidationArrayArgument(objectValueWithVariableContext, "anyOf")
                .ifPresent(arrayValueWithVariableContext -> {
                            JsonArray anyOf = new JsonArray();
                            arrayValueWithVariableContext.valueWithVariable().stream()
                                    .filter(valueWithVariableContext -> valueWithVariableContext.objectValueWithVariable() != null)
                                    .forEach(valueWithVariableContext -> anyOf.add(buildValidation(valueWithVariableContext.objectValueWithVariable())));
                            validation.add("anyOf", anyOf);
                        }
                );

        getValidationArrayArgument(objectValueWithVariableContext, "oneOf")
                .ifPresent(arrayValueWithVariableContext -> {
                            JsonArray oneOf = new JsonArray();
                            arrayValueWithVariableContext.valueWithVariable().stream()
                                    .filter(valueWithVariableContext -> valueWithVariableContext.objectValueWithVariable() != null)
                                    .forEach(valueWithVariableContext -> oneOf.add(buildValidation(valueWithVariableContext.objectValueWithVariable())));
                            validation.add("oneOf", oneOf);
                        }
                );

        getValidationObjectArgument(objectValueWithVariableContext, "not")
                .ifPresent(not -> validation.add("not", buildValidation(not)));

        getValidationArrayArgument(objectValueWithVariableContext, "properties")
                .ifPresent(arrayValueWithVariableContext -> validation.add("properties", buildProperties(arrayValueWithVariableContext)));

        getValidationArrayArgument(objectValueWithVariableContext, "if")
                .ifPresent(arrayValueWithVariableContext -> validation.add("if", buildProperties(arrayValueWithVariableContext)));

        getValidationArrayArgument(objectValueWithVariableContext, "then")
                .ifPresent(arrayValueWithVariableContext -> validation.add("then", buildProperties(arrayValueWithVariableContext)));

        getValidationArrayArgument(objectValueWithVariableContext, "else")
                .ifPresent(arrayValueWithVariableContext -> validation.add("else", buildProperties(arrayValueWithVariableContext)));

        getValidationArrayArgument(objectValueWithVariableContext, "dependentRequired")
                .ifPresent(arrayValueWithVariableContext -> validation.add("dependentRequired", buildDependentRequired(arrayValueWithVariableContext)));

        return validation;
    }

    protected JsonObject buildProperties(GraphqlParser.ArrayValueWithVariableContext arrayValueWithVariableContext) {
        JsonObject properties = new JsonObject();
        arrayValueWithVariableContext.valueWithVariable().stream()
                .filter(property -> property.objectValueWithVariable() != null)
                .forEach(property ->
                        getValidationStringArgument(property.objectValueWithVariable(), "name")
                                .ifPresent(name ->
                                        getValidationObjectArgument(property.objectValueWithVariable(), "validation")
                                                .ifPresent(validation ->
                                                        properties.add(name, buildValidation(validation))
                                                )
                                )
                );
        return properties;
    }

    protected JsonObject buildDependentRequired(GraphqlParser.ArrayValueWithVariableContext arrayValueWithVariableContext) {
        JsonObject dependentRequired = new JsonObject();
        arrayValueWithVariableContext.valueWithVariable().stream()
                .filter(property -> property.objectValueWithVariable() != null)
                .forEach(property ->
                        getValidationStringArgument(property.objectValueWithVariable(), "name")
                                .ifPresent(name ->
                                        getValidationArrayArgument(property.objectValueWithVariable(), "required")
                                                .ifPresent(required -> {
                                                            JsonArray jsonArray = new JsonArray();
                                                            required.valueWithVariable().stream()
                                                                    .filter(item -> item.StringValue() != null)
                                                                    .forEach(item -> jsonArray.add(DOCUMENT_UTIL.getStringValue(item.StringValue())));
                                                            dependentRequired.add(name, jsonArray);
                                                        }
                                                )
                                )
                );
        return dependentRequired;
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
