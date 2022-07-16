package io.graphoenix.core.schema;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonMetaSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.ValidationMessage;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.core.manager.GraphQLDocumentManager;
import io.graphoenix.spi.dto.GraphQLError;
import io.vavr.control.Try;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.*;
import jakarta.json.spi.JsonProvider;

import java.util.Set;
import java.util.stream.Collectors;

import static io.graphoenix.core.error.GraphQLErrorType.FIELD_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.MUTATION_TYPE_NOT_EXIST;
import static io.graphoenix.core.utils.DocumentUtil.DOCUMENT_UTIL;
import static jakarta.json.JsonValue.*;

@ApplicationScoped
public class JsonSchemaValidator {

    private final ObjectMapper mapper = new ObjectMapper();
    private final GraphQLDocumentManager manager;
    private final JsonProvider jsonProvider;
    private final JsonSchemaManager jsonSchemaManager;
    private final JsonSchemaFactory factory;

    @Inject
    public JsonSchemaValidator(JsonSchemaManager jsonSchemaManager, GraphQLDocumentManager manager, JsonProvider jsonProvider, JsonSchemaResourceURNFactory jsonSchemaResourceURNFactory) {
        this.manager = manager;
        this.jsonSchemaManager = jsonSchemaManager;
        this.jsonProvider = jsonProvider;
        JsonMetaSchema jsonMetaSchema = JsonMetaSchema.getV201909();
        this.factory = new JsonSchemaFactory.Builder().defaultMetaSchemaURI(jsonMetaSchema.getUri()).addMetaSchema(jsonMetaSchema).addUrnFactory(jsonSchemaResourceURNFactory).build();
    }

    public Set<ValidationMessage> validate(String objectName, String json) throws JsonProcessingException {
        return factory.getSchema(jsonSchemaManager.getJsonSchema(objectName)).validate(mapper.readTree(json));
    }

    public void validateOperation(GraphqlParser.OperationDefinitionContext operationDefinitionContext) {
        Set<ValidationMessage> messageSet = operationDefinitionContext.selectionSet().selection().stream()
                .flatMap(selectionContext -> Try.of(() -> validateSelection(selectionContext)).get().stream())
                .collect(Collectors.toSet());

        if (messageSet.size() > 0) {
            GraphQLErrors graphQLErrors = new GraphQLErrors();
            messageSet.forEach(validationMessage ->
                    graphQLErrors.add(
                            new GraphQLError(validationMessage.getMessage())
                                    .setPath(validationMessage.getPath())
                                    .setSchemaPath(validationMessage.getSchemaPath())
                    )
            );
            throw graphQLErrors;
        }
    }

    protected Set<ValidationMessage> validateSelection(GraphqlParser.SelectionContext selectionContext) throws JsonProcessingException {
        String mutationTypeName = manager.getMutationOperationTypeName().orElseThrow(() -> new GraphQLErrors(MUTATION_TYPE_NOT_EXIST));
        GraphqlParser.FieldDefinitionContext fieldDefinitionContext = manager.getField(mutationTypeName,
                selectionContext.field().name().getText()).orElseThrow(() -> new GraphQLErrors(FIELD_NOT_EXIST.bind(mutationTypeName, selectionContext.field().name().getText())));
        String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
        return validate(fieldTypeName, argumentsToJsonElement(selectionContext.field().arguments()).toString());
    }

    protected JsonValue argumentsToJsonElement(GraphqlParser.ArgumentsContext argumentsContext) {
        JsonObjectBuilder jsonObjectBuilder = jsonProvider.createObjectBuilder();
        if (argumentsContext != null) {
            argumentsContext.argument().stream()
                    .filter(argumentContext -> argumentContext.valueWithVariable().NullValue() == null)
                    .forEach(argumentContext -> jsonObjectBuilder.add(argumentContext.name().getText(), valueWithVariableToJsonElement(argumentContext.valueWithVariable())));
        }
        return jsonObjectBuilder.build();
    }

    protected JsonValue objectValueWithVariableToJsonElement(GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {
        JsonObjectBuilder jsonObjectBuilder = jsonProvider.createObjectBuilder();
        if (objectValueWithVariableContext.objectFieldWithVariable() != null) {
            objectValueWithVariableContext.objectFieldWithVariable().stream()
                    .filter(objectFieldWithVariableContext -> objectFieldWithVariableContext.valueWithVariable().NullValue() == null)
                    .forEach(objectFieldWithVariableContext -> jsonObjectBuilder.add(objectFieldWithVariableContext.name().getText(), valueWithVariableToJsonElement(objectFieldWithVariableContext.valueWithVariable())));
        }
        return jsonObjectBuilder.build();
    }

    protected JsonValue valueWithVariableToJsonElement(GraphqlParser.ValueWithVariableContext valueWithVariableContext) {
        if (valueWithVariableContext.BooleanValue() != null) {
            return Boolean.parseBoolean(valueWithVariableContext.BooleanValue().getText()) ? TRUE : FALSE;
        } else if (valueWithVariableContext.IntValue() != null) {
            return jsonProvider.createValue(Integer.parseInt(valueWithVariableContext.IntValue().getText()));
        } else if (valueWithVariableContext.FloatValue() != null) {
            return jsonProvider.createValue(Float.parseFloat(valueWithVariableContext.FloatValue().getText()));
        } else if (valueWithVariableContext.StringValue() != null) {
            return jsonProvider.createValue((DOCUMENT_UTIL.getStringValue(valueWithVariableContext.StringValue())));
        } else if (valueWithVariableContext.enumValue() != null) {
            return jsonProvider.createValue(valueWithVariableContext.enumValue().getText());
        } else if (valueWithVariableContext.objectValueWithVariable() != null) {
            return objectValueWithVariableToJsonElement(valueWithVariableContext.objectValueWithVariable());
        } else if (valueWithVariableContext.arrayValueWithVariable() != null) {
            JsonArrayBuilder jsonArrayBuilder = jsonProvider.createArrayBuilder();
            valueWithVariableContext.arrayValueWithVariable().valueWithVariable().stream()
                    .filter(subValueWithVariableContext -> subValueWithVariableContext.NullValue() == null)
                    .forEach(subValueWithVariableContext -> jsonArrayBuilder.add(valueWithVariableToJsonElement(subValueWithVariableContext)));
            return jsonArrayBuilder.build();
        } else if (valueWithVariableContext.NullValue() != null) {
            return NULL;
        }
        return null;
    }
}
