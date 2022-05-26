package io.graphoenix.json.schema.translator;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Optional;

import static io.graphoenix.core.error.GraphQLErrorType.UNSUPPORTED_FIELD_TYPE;
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
        JsonObject property = new JsonObject();
        if (manager.fieldTypeIsList(fieldDefinitionContext.type())) {
            property.addProperty("type", "array");
            JsonObject items = new JsonObject();
            items.addProperty("type", fieldTypeToPropertyType(fieldDefinitionContext.type()));
            buildValidation(fieldDefinitionContext, items);
            property.add("items", items);
        } else {
            property.addProperty("type", fieldTypeToPropertyType(fieldDefinitionContext.type()));
            buildValidation(fieldDefinitionContext, property);
        }
        return property;
    }

    protected void buildValidation(GraphqlParser.FieldDefinitionContext fieldDefinitionContext, JsonObject jsonObject) {
        String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
        switch (fieldTypeName) {
            case "ID":
            case "String":
            case "Date":
            case "Time":
            case "DateTime":
            case "Timestamp":
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
                        });
                break;
            case "Boolean":
                break;
            case "Int":
            case "BigInteger":
            case "Float":
            case "BigDecimal":
                getValidationDirectiveContext(fieldDefinitionContext.directives())
                        .ifPresent(directiveContext -> {
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
                        });
                break;
        }
        throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE.bind(fieldDefinitionContext.getText()));
    }

    protected String fieldTypeToPropertyType(GraphqlParser.TypeContext typeContext) {
        String fieldTypeName = manager.getFieldTypeName(typeContext);
        switch (fieldTypeName) {
            case "ID":
            case "String":
            case "Date":
            case "Time":
            case "DateTime":
            case "Timestamp":
                return "string";
            case "Boolean":
                return "boolean";
            case "Int":
            case "BigInteger":
            case "Float":
            case "BigDecimal":
                return "number";
        }
        throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE.bind(typeContext));
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
