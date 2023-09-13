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
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.JsonValue;
import jakarta.json.spi.JsonProvider;
import jakarta.json.stream.JsonCollectors;

import java.util.AbstractMap;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.graphoenix.core.error.GraphQLErrorType.MUTATION_TYPE_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.QUERY_TYPE_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.SUBSCRIBE_TYPE_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.UNSUPPORTED_OPERATION_TYPE;
import static io.graphoenix.core.utils.DocumentUtil.DOCUMENT_UTIL;
import static io.graphoenix.spi.constant.Hammurabi.ARGUMENTS_SUFFIX;
import static io.graphoenix.spi.constant.Hammurabi.WHERE_INPUT_NAME;
import static jakarta.json.JsonValue.FALSE;
import static jakarta.json.JsonValue.NULL;
import static jakarta.json.JsonValue.TRUE;

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

    public Set<ValidationMessage> validate(String schemaName, String json) throws JsonProcessingException {
        return factory.getSchema(jsonSchemaManager.getJsonSchema(schemaName)).validate(mapper.readTree(json));
    }

    public void validateOperation(GraphqlParser.OperationDefinitionContext operationDefinitionContext) {
        Set<ValidationMessage> messageSet = operationDefinitionContext.selectionSet().selection().stream()
                .filter(selectionContext -> selectionContext.field().arguments() != null)
                .flatMap(selectionContext -> validateSelection(operationDefinitionContext, selectionContext))
                .collect(Collectors.toSet());

        if (messageSet.size() > 0) {
            GraphQLErrors graphQLErrors = new GraphQLErrors();
            messageSet.forEach(validationMessage -> graphQLErrors.add(new GraphQLError(validationMessage.getMessage()).setSchemaPath(validationMessage.getSchemaPath())));
            throw graphQLErrors;
        }
    }

    protected Stream<ValidationMessage> validateSelection(GraphqlParser.OperationDefinitionContext operationDefinitionContext, GraphqlParser.SelectionContext selectionContext) {
        JsonValue jsonValue = argumentsToJsonElement(selectionContext.field().arguments());
        String schemaName;
        try {
            if (operationDefinitionContext.operationType() == null || operationDefinitionContext.operationType().QUERY() != null) {
                String operationTypeName = manager.getQueryOperationTypeName().orElseThrow(() -> new GraphQLErrors(QUERY_TYPE_NOT_EXIST));
                schemaName = operationTypeName + "_" + selectionContext.field().name().getText() + "_" + ARGUMENTS_SUFFIX;
            } else if (operationDefinitionContext.operationType().SUBSCRIPTION() != null) {
                String operationTypeName = manager.getSubscriptionOperationTypeName().orElseThrow(() -> new GraphQLErrors(SUBSCRIBE_TYPE_NOT_EXIST));
                schemaName = operationTypeName + "_" + selectionContext.field().name().getText() + "_" + ARGUMENTS_SUFFIX;
            } else if (operationDefinitionContext.operationType().MUTATION() != null) {
                String operationTypeName = manager.getMutationOperationTypeName().orElseThrow(() -> new GraphQLErrors(MUTATION_TYPE_NOT_EXIST));
                if (selectionContext.field().arguments().argument().stream().anyMatch(argumentContext -> argumentContext.name().getText().equals(WHERE_INPUT_NAME))) {
                    schemaName = operationTypeName + "_" + selectionContext.field().name().getText() + "_" + ARGUMENTS_SUFFIX + "_update";
                } else {
                    schemaName = operationTypeName + "_" + selectionContext.field().name().getText() + "_" + ARGUMENTS_SUFFIX;
                }
            } else {
                throw new GraphQLErrors(UNSUPPORTED_OPERATION_TYPE.bind(operationDefinitionContext.operationType().getText()));
            }
            return validate(schemaName, jsonValue.toString()).stream();
        } catch (JsonProcessingException e) {
            throw new GraphQLErrors(e);
        }
    }

    protected JsonValue argumentsToJsonElement(GraphqlParser.ArgumentsContext argumentsContext) {
        if (argumentsContext != null) {
            return argumentsContext.argument().stream()
//                    .filter(argumentContext -> argumentContext.valueWithVariable().NullValue() == null)
//                    .filter(argumentContext -> Arrays.stream(SCHEMA_EXCLUDE_INPUT).noneMatch(inputName -> inputName.equals(argumentContext.name().getText())))
                    .map(argumentContext -> new AbstractMap.SimpleEntry<>(argumentContext.name().getText(), valueWithVariableToJsonElement(argumentContext.valueWithVariable())))
                    .collect(JsonCollectors.toJsonObject());
        }
        return NULL;
    }

    protected JsonValue objectValueWithVariableToJsonElement(GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {
        if (objectValueWithVariableContext.objectFieldWithVariable() != null) {
            return objectValueWithVariableContext.objectFieldWithVariable().stream()
//                    .filter(objectFieldWithVariableContext -> objectFieldWithVariableContext.valueWithVariable().NullValue() == null)
//                    .filter(objectFieldWithVariableContext -> Arrays.stream(SCHEMA_EXCLUDE_INPUT).noneMatch(inputName -> inputName.equals(objectFieldWithVariableContext.name().getText())))
                    .map(objectFieldWithVariableContext -> new AbstractMap.SimpleEntry<>(objectFieldWithVariableContext.name().getText(), valueWithVariableToJsonElement(objectFieldWithVariableContext.valueWithVariable())))
                    .collect(JsonCollectors.toJsonObject());
        }
        return NULL;
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
            return valueWithVariableContext.arrayValueWithVariable().valueWithVariable().stream()
//                    .filter(subValueWithVariableContext -> subValueWithVariableContext.NullValue() == null)
                    .map(this::valueWithVariableToJsonElement)
                    .collect(JsonCollectors.toJsonArray());
        } else if (valueWithVariableContext.NullValue() != null) {
            return NULL;
        }
        return null;
    }
}
