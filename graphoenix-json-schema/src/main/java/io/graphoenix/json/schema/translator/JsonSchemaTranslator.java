package io.graphoenix.json.schema.translator;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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

    private final IGraphQLDocumentManager manager;

    @Inject
    public JsonSchemaTranslator(GraphQLConfig graphQLConfig,
                                IGraphQLDocumentManager manager) {
        this.graphQLConfig = graphQLConfig;
        this.manager = manager;
    }

    protected JsonElement objectToJsonSchema(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {

        JsonObject jsonSchema = new JsonObject();
        jsonSchema.addProperty("type", "object");
        jsonSchema.add("properties", objectToProperties(objectTypeDefinitionContext));

        jsonSchema.addProperty("additionalProperties", false);
        return jsonSchema;
    }

    protected JsonElement objectToProperties(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {

        JsonObject properties = new JsonObject();
        objectTypeDefinitionContext.fieldsDefinition().fieldDefinition()
                .forEach(fieldDefinitionContext -> {
                            properties.add(fieldDefinitionContext.name().getText(), fieldToProperty(fieldDefinitionContext));
                        }
                );
        return properties;
    }

    protected JsonElement fieldToProperty(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        if (manager.fieldTypeIsList(fieldDefinitionContext.type())) {
            JsonObject property = new JsonObject();
            property.addProperty("type", "array");
            property.add("items", buildValidation(fieldDefinitionContext));
            getValidationDirectiveContext(fieldDefinitionContext.directives())
                    .ifPresent(directiveContext -> {
                                getValidationIntArgument(directiveContext, "items")
                                        .ifPresent(minItems -> property.addProperty("minItems", minItems));
                                getValidationIntArgument(directiveContext, "minItems")
                                        .ifPresent(minItems -> property.addProperty("minItems", minItems));
                                getValidationIntArgument(directiveContext, "maxItems")
                                        .ifPresent(maxItems -> property.addProperty("maxItems", maxItems));
                                getValidationBooleanArgument(directiveContext, "uniqueItems")
                                        .ifPresent(uniqueItems -> property.addProperty("uniqueItems", uniqueItems));
                            }
                    );
            return property;
        } else {
            return buildValidation(fieldDefinitionContext);
        }
    }

    protected JsonElement buildValidation(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        JsonObject jsonObject = new JsonObject();
        String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
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

        if (manager.fieldTypeIsList(fieldDefinitionContext.type())) {
            getValidationDirectiveContext(fieldDefinitionContext.directives())
                    .flatMap(directiveContext -> getValidationObjectArgument(directiveContext, "items"))
                    .ifPresent(objectValueWithVariableContext -> {
                        getValidationIntArgument(objectValueWithVariableContext, "minLength")
                                .ifPresent(minLength -> jsonObject.addProperty("minLength", minLength));
                        getValidationIntArgument(objectValueWithVariableContext, "maxLength")
                                .ifPresent(maxLength -> jsonObject.addProperty("maxLength", maxLength));
                        getValidationStringArgument(objectValueWithVariableContext, "pattern")
                                .ifPresent(pattern -> jsonObject.addProperty("pattern", pattern));
                        getValidationStringArgument(objectValueWithVariableContext, "format")
                                .ifPresent(format -> jsonObject.addProperty("format", format));
                        getValidationStringArgument(objectValueWithVariableContext, "contentMediaType")
                                .ifPresent(contentMediaType -> jsonObject.addProperty("contentMediaType", contentMediaType));
                        getValidationStringArgument(objectValueWithVariableContext, "contentEncoding")
                                .ifPresent(contentEncoding -> jsonObject.addProperty("contentEncoding", contentEncoding));

                        getValidationFloatArgument(objectValueWithVariableContext, "minimum")
                                .ifPresent(minimum -> jsonObject.addProperty("minimum", minimum));
                        getValidationFloatArgument(objectValueWithVariableContext, "exclusiveMinimum")
                                .ifPresent(exclusiveMinimum -> jsonObject.addProperty("exclusiveMinimum", exclusiveMinimum));
                        getValidationFloatArgument(objectValueWithVariableContext, "maximum")
                                .ifPresent(maximum -> jsonObject.addProperty("maximum", maximum));
                        getValidationFloatArgument(objectValueWithVariableContext, "exclusiveMaximum")
                                .ifPresent(exclusiveMaximum -> jsonObject.addProperty("exclusiveMaximum", exclusiveMaximum));
                        getValidationFloatArgument(objectValueWithVariableContext, "multipleOf")
                                .ifPresent(multipleOf -> jsonObject.addProperty("multipleOf", multipleOf));

                        getValidationStringArgument(objectValueWithVariableContext, "const")
                                .ifPresent(constValue -> jsonObject.addProperty("const", constValue));

                        getValidationArrayArgument(objectValueWithVariableContext, "allOf")
                                .ifPresent(arrayValueWithVariableContext -> {
                                            JsonArray allOf = new JsonArray();
                                            arrayValueWithVariableContext.valueWithVariable().stream()
                                                    .filter(valueWithVariableContext -> valueWithVariableContext.objectValueWithVariable() != null)
                                                    .forEach(valueWithVariableContext -> allOf.add(buildValidation(valueWithVariableContext.objectValueWithVariable())));
                                            jsonObject.add("allOf", allOf);
                                        }
                                );

                        getValidationArrayArgument(objectValueWithVariableContext, "anyOf")
                                .ifPresent(arrayValueWithVariableContext -> {
                                            JsonArray anyOf = new JsonArray();
                                            arrayValueWithVariableContext.valueWithVariable().stream()
                                                    .filter(valueWithVariableContext -> valueWithVariableContext.objectValueWithVariable() != null)
                                                    .forEach(valueWithVariableContext -> anyOf.add(buildValidation(valueWithVariableContext.objectValueWithVariable())));
                                            jsonObject.add("anyOf", anyOf);
                                        }
                                );

                        getValidationArrayArgument(objectValueWithVariableContext, "oneOf")
                                .ifPresent(arrayValueWithVariableContext -> {
                                            JsonArray oneOf = new JsonArray();
                                            arrayValueWithVariableContext.valueWithVariable().stream()
                                                    .filter(valueWithVariableContext -> valueWithVariableContext.objectValueWithVariable() != null)
                                                    .forEach(valueWithVariableContext -> oneOf.add(buildValidation(valueWithVariableContext.objectValueWithVariable())));
                                            jsonObject.add("oneOf", oneOf);
                                        }
                                );

                        getValidationObjectArgument(objectValueWithVariableContext, "not")
                                .ifPresent(not -> jsonObject.add("not", buildValidation(not)));
                    });
        } else {
            getValidationDirectiveContext(fieldDefinitionContext.directives())
                    .ifPresent(directiveContext -> {
                                getValidationIntArgument(directiveContext, "minLength")
                                        .ifPresent(minLength -> jsonObject.addProperty("minLength", minLength));
                                getValidationIntArgument(directiveContext, "maxLength")
                                        .ifPresent(maxLength -> jsonObject.addProperty("maxLength", maxLength));
                                getValidationStringArgument(directiveContext, "pattern")
                                        .ifPresent(pattern -> jsonObject.addProperty("pattern", pattern));
                                getValidationStringArgument(directiveContext, "format")
                                        .ifPresent(format -> jsonObject.addProperty("format", format));
                                getValidationStringArgument(directiveContext, "contentMediaType")
                                        .ifPresent(contentMediaType -> jsonObject.addProperty("contentMediaType", contentMediaType));
                                getValidationStringArgument(directiveContext, "contentEncoding")
                                        .ifPresent(contentEncoding -> jsonObject.addProperty("contentEncoding", contentEncoding));

                                getValidationFloatArgument(directiveContext, "minimum")
                                        .ifPresent(minimum -> jsonObject.addProperty("minimum", minimum));
                                getValidationFloatArgument(directiveContext, "exclusiveMinimum")
                                        .ifPresent(exclusiveMinimum -> jsonObject.addProperty("exclusiveMinimum", exclusiveMinimum));
                                getValidationFloatArgument(directiveContext, "maximum")
                                        .ifPresent(maximum -> jsonObject.addProperty("maximum", maximum));
                                getValidationFloatArgument(directiveContext, "exclusiveMaximum")
                                        .ifPresent(exclusiveMaximum -> jsonObject.addProperty("exclusiveMaximum", exclusiveMaximum));
                                getValidationFloatArgument(directiveContext, "multipleOf")
                                        .ifPresent(multipleOf -> jsonObject.addProperty("multipleOf", multipleOf));

                                getValidationStringArgument(directiveContext, "const")
                                        .ifPresent(constValue -> jsonObject.addProperty("const", constValue));

                                getValidationArrayArgument(directiveContext, "allOf")
                                        .ifPresent(arrayValueWithVariableContext -> {
                                                    JsonArray allOf = new JsonArray();
                                                    arrayValueWithVariableContext.valueWithVariable().stream()
                                                            .filter(valueWithVariableContext -> valueWithVariableContext.objectValueWithVariable() != null)
                                                            .forEach(valueWithVariableContext -> allOf.add(buildValidation(valueWithVariableContext.objectValueWithVariable())));
                                                    jsonObject.add("allOf", allOf);
                                                }
                                        );

                                getValidationArrayArgument(directiveContext, "anyOf")
                                        .ifPresent(arrayValueWithVariableContext -> {
                                                    JsonArray anyOf = new JsonArray();
                                                    arrayValueWithVariableContext.valueWithVariable().stream()
                                                            .filter(valueWithVariableContext -> valueWithVariableContext.objectValueWithVariable() != null)
                                                            .forEach(valueWithVariableContext -> anyOf.add(buildValidation(valueWithVariableContext.objectValueWithVariable())));
                                                    jsonObject.add("anyOf", anyOf);
                                                }
                                        );

                                getValidationArrayArgument(directiveContext, "oneOf")
                                        .ifPresent(arrayValueWithVariableContext -> {
                                                    JsonArray oneOf = new JsonArray();
                                                    arrayValueWithVariableContext.valueWithVariable().stream()
                                                            .filter(valueWithVariableContext -> valueWithVariableContext.objectValueWithVariable() != null)
                                                            .forEach(valueWithVariableContext -> oneOf.add(buildValidation(valueWithVariableContext.objectValueWithVariable())));
                                                    jsonObject.add("oneOf", oneOf);
                                                }
                                        );

                                getValidationObjectArgument(directiveContext, "not")
                                        .ifPresent(not -> jsonObject.add("not", buildValidation(not)));
                            }
                    );
        }

        return jsonObject;
    }

    protected JsonObject buildValidation(GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {

        JsonObject jsonObject = new JsonObject();

        getValidationIntArgument(objectValueWithVariableContext, "minLength")
                .ifPresent(minLength -> jsonObject.addProperty("minLength", minLength));
        getValidationIntArgument(objectValueWithVariableContext, "maxLength")
                .ifPresent(maxLength -> jsonObject.addProperty("maxLength", maxLength));
        getValidationStringArgument(objectValueWithVariableContext, "pattern")
                .ifPresent(pattern -> jsonObject.addProperty("pattern", pattern));
        getValidationStringArgument(objectValueWithVariableContext, "format")
                .ifPresent(format -> jsonObject.addProperty("format", format));
        getValidationStringArgument(objectValueWithVariableContext, "contentMediaType")
                .ifPresent(contentMediaType -> jsonObject.addProperty("contentMediaType", contentMediaType));
        getValidationStringArgument(objectValueWithVariableContext, "contentEncoding")
                .ifPresent(contentEncoding -> jsonObject.addProperty("contentEncoding", contentEncoding));

        getValidationFloatArgument(objectValueWithVariableContext, "minimum")
                .ifPresent(minimum -> jsonObject.addProperty("minimum", minimum));
        getValidationFloatArgument(objectValueWithVariableContext, "exclusiveMinimum")
                .ifPresent(exclusiveMinimum -> jsonObject.addProperty("exclusiveMinimum", exclusiveMinimum));
        getValidationFloatArgument(objectValueWithVariableContext, "maximum")
                .ifPresent(maximum -> jsonObject.addProperty("maximum", maximum));
        getValidationFloatArgument(objectValueWithVariableContext, "exclusiveMaximum")
                .ifPresent(exclusiveMaximum -> jsonObject.addProperty("exclusiveMaximum", exclusiveMaximum));
        getValidationFloatArgument(objectValueWithVariableContext, "multipleOf")
                .ifPresent(multipleOf -> jsonObject.addProperty("multipleOf", multipleOf));

        getValidationStringArgument(objectValueWithVariableContext, "const")
                .ifPresent(constValue -> jsonObject.addProperty("const", constValue));

        getValidationArrayArgument(objectValueWithVariableContext, "allOf")
                .ifPresent(arrayValueWithVariableContext -> {
                            JsonArray allOf = new JsonArray();
                            arrayValueWithVariableContext.valueWithVariable().stream()
                                    .filter(valueWithVariableContext -> valueWithVariableContext.objectValueWithVariable() != null)
                                    .forEach(valueWithVariableContext -> allOf.add(buildValidation(valueWithVariableContext.objectValueWithVariable())));
                            jsonObject.add("allOf", allOf);
                        }
                );

        getValidationArrayArgument(objectValueWithVariableContext, "anyOf")
                .ifPresent(arrayValueWithVariableContext -> {
                            JsonArray anyOf = new JsonArray();
                            arrayValueWithVariableContext.valueWithVariable().stream()
                                    .filter(valueWithVariableContext -> valueWithVariableContext.objectValueWithVariable() != null)
                                    .forEach(valueWithVariableContext -> anyOf.add(buildValidation(valueWithVariableContext.objectValueWithVariable())));
                            jsonObject.add("anyOf", anyOf);
                        }
                );

        getValidationArrayArgument(objectValueWithVariableContext, "oneOf")
                .ifPresent(arrayValueWithVariableContext -> {
                            JsonArray oneOf = new JsonArray();
                            arrayValueWithVariableContext.valueWithVariable().stream()
                                    .filter(valueWithVariableContext -> valueWithVariableContext.objectValueWithVariable() != null)
                                    .forEach(valueWithVariableContext -> oneOf.add(buildValidation(valueWithVariableContext.objectValueWithVariable())));
                            jsonObject.add("oneOf", oneOf);
                        }
                );

        getValidationObjectArgument(objectValueWithVariableContext, "not")
                .ifPresent(not -> jsonObject.add("not", buildValidation(not)));

        return jsonObject;
    }


    protected Optional<GraphqlParser.DirectiveContext> getValidationDirectiveContext(GraphqlParser.DirectivesContext directivesContext) {
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
