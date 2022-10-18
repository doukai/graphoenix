package io.graphoenix.core.schema;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Streams;
import com.networknt.schema.JsonMetaSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.ValidationMessage;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.core.manager.GraphQLDocumentManager;
import io.graphoenix.spi.constant.Hammurabi;
import io.graphoenix.spi.dto.GraphQLError;
import io.vavr.control.Try;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.*;
import jakarta.json.spi.JsonProvider;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.graphoenix.core.error.GraphQLErrorType.FIELD_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.MUTATION_TYPE_NOT_EXIST;
import static io.graphoenix.core.utils.DocumentUtil.DOCUMENT_UTIL;
import static io.graphoenix.spi.constant.Hammurabi.EXCLUDE_INPUT;
import static io.graphoenix.spi.constant.Hammurabi.MutationType.MERGE;
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

    public Set<ValidationMessage> validate(String objectName, boolean isList, String json) throws JsonProcessingException {
        return factory.getSchema(jsonSchemaManager.getJsonSchema(isList ? objectName.concat("List") : objectName)).validate(mapper.readTree(json));
    }

    public Set<ValidationMessage> validateUpdate(String objectName, boolean isList, String json) throws JsonProcessingException {
        JsonNode jsonNode = mapper.readTree(json);
        if (manager.isObject(objectName)) {
            if (isList) {
                return Streams.stream(jsonNode.elements())
                        .flatMap(item -> {
                                    try {
                                        return validateUpdate(item, objectName);
                                    } catch (JsonProcessingException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                        ).collect(Collectors.toSet());
            }
            return validateUpdate(jsonNode, objectName).collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }

    public Stream<ValidationMessage> validateUpdate(JsonNode jsonNode, String objectName) throws JsonProcessingException {
        ObjectNode jsonSchema = (ObjectNode) mapper.readTree(jsonSchemaManager.getJsonSchema(objectName));
        jsonSchema.remove("required");
        return Stream.concat(
                factory.getSchema(jsonSchema)
                        .validate(
                                mapper.createObjectNode()
                                        .setAll(
                                                Streams.stream(jsonNode.fields())
                                                        .filter(field -> !manager.isObject(manager.getFieldTypeName(manager.getField(objectName, field.getKey()).orElseThrow(() -> new GraphQLErrors(FIELD_NOT_EXIST.bind(objectName, field.getKey()))).type())))
                                                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                                        )
                        ).stream(),
                Streams.stream(jsonNode.fields())
                        .filter(field -> manager.isObject(manager.getFieldTypeName(manager.getField(objectName, field.getKey()).orElseThrow(() -> new GraphQLErrors(FIELD_NOT_EXIST.bind(objectName, field.getKey()))).type())))
                        .flatMap(field -> {
                                    try {
                                        if (manager.fieldTypeIsList(manager.getField(objectName, field.getKey()).orElseThrow(() -> new GraphQLErrors(FIELD_NOT_EXIST.bind(objectName, field.getKey()))).type())) {
                                            return Streams.stream(jsonNode.elements())
                                                    .flatMap(item -> {
                                                                try {
                                                                    return validateUpdate(item, objectName);
                                                                } catch (JsonProcessingException e) {
                                                                    throw new RuntimeException(e);
                                                                }
                                                            }
                                                    );
                                        }
                                        return validateUpdate(field.getValue(), manager.getFieldTypeName(manager.getField(objectName, field.getKey()).orElseThrow(() -> new GraphQLErrors(FIELD_NOT_EXIST.bind(objectName, field.getKey()))).type()));
                                    } catch (JsonProcessingException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                        )
        );
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
        boolean isList = manager.fieldTypeIsList(fieldDefinitionContext.type());

        Hammurabi.MutationType mutationType = manager.getMutationType(selectionContext);
        if (mutationType.equals(MERGE)) {
            return validate(fieldTypeName, isList, argumentsToJsonElement(selectionContext.field().arguments()).toString());
        } else {
            return validateUpdate(fieldTypeName, isList, argumentsToJsonElement(selectionContext.field().arguments()).toString());
        }
    }

    protected JsonValue argumentsToJsonElement(GraphqlParser.ArgumentsContext argumentsContext) {
        JsonObjectBuilder jsonObjectBuilder = jsonProvider.createObjectBuilder();
        if (argumentsContext != null) {
            argumentsContext.argument().stream()
                    .filter(argumentContext -> argumentContext.valueWithVariable().NullValue() == null)
                    .filter(argumentContext -> Arrays.stream(EXCLUDE_INPUT).noneMatch(inputName -> inputName.equals(argumentContext.name().getText())))
                    .forEach(argumentContext -> jsonObjectBuilder.add(argumentContext.name().getText(), valueWithVariableToJsonElement(argumentContext.valueWithVariable())));
        }
        return jsonObjectBuilder.build();
    }

    protected JsonValue objectValueWithVariableToJsonElement(GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {
        JsonObjectBuilder jsonObjectBuilder = jsonProvider.createObjectBuilder();
        if (objectValueWithVariableContext.objectFieldWithVariable() != null) {
            objectValueWithVariableContext.objectFieldWithVariable().stream()
                    .filter(objectFieldWithVariableContext -> objectFieldWithVariableContext.valueWithVariable().NullValue() == null)
                    .filter(objectFieldWithVariableContext -> Arrays.stream(EXCLUDE_INPUT).noneMatch(inputName -> inputName.equals(objectFieldWithVariableContext.name().getText())))
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
