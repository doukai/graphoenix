package io.graphoenix.core.handler;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.core.error.GraphQLErrorType;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.core.operation.Field;
import io.graphoenix.core.operation.Operation;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.handler.OperationHandler;
import jakarta.json.JsonValue;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import static io.graphoenix.spi.constant.Hammurabi.EXCLUDE_INPUT;

public abstract class OperationSubscriber {

    private final Map<String, List<Field>> filterSelectionList = new ConcurrentHashMap<>();

    private final IGraphQLDocumentManager manager;

    public OperationSubscriber() {
        this.manager = BeanContext.get(IGraphQLDocumentManager.class);
    }

    public Operation buildIDSelection(Operation operation) {
        String subscriptionTypeName = manager.getSubscriptionOperationTypeName()
                .orElseThrow(() -> new GraphQLErrors(GraphQLErrorType.SUBSCRIBE_TYPE_NOT_EXIST));
        buildIDSelection(subscriptionTypeName, operation.getFields());
        return operation;
    }

    public Operation buildIDSelection(GraphqlParser.OperationDefinitionContext operationDefinitionContext) {
        Operation operation = new Operation(operationDefinitionContext);
        return buildIDSelection(operation);
    }

    private void buildIDSelection(String typeName, Collection<Field> fields) {
        for (Field field : fields) {
            GraphqlParser.FieldDefinitionContext fieldDefinitionContext = manager.getField(typeName, field.getName())
                    .orElseThrow(() -> new GraphQLErrors(GraphQLErrorType.FIELD_NOT_EXIST.bind(typeName, field.getName())));
            String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
            if (field.getFields() != null) {
                Optional<String> idFieldName = manager.getObjectTypeIDFieldName(fieldTypeName);
                idFieldName.ifPresent(name -> Field.mergeSelection(field.getFields(), Collections.singleton(new Field(name))));
                buildIDSelection(fieldTypeName, field.getFields());
            }
        }
    }

    public Operation buildSubscriptionFilterSelection(Operation operation) {
        for (Field field : operation.getFields()) {
            String subscriptionOperationTypeName = manager.getSubscriptionOperationTypeName().orElseThrow(() -> new GraphQLErrors(GraphQLErrorType.SUBSCRIBE_TYPE_NOT_EXIST));
            manager.getField(subscriptionOperationTypeName, field.getName())
                    .ifPresent(fieldDefinitionContext -> {
                                String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
                                Field.mergeSelection(field.getFields(), filterSelectionList.get(fieldTypeName));
                            }
                    );
        }
        return operation;
    }

    public void registerSelection(GraphqlParser.OperationDefinitionContext operationDefinitionContext) {
        for (GraphqlParser.SelectionContext selectionContext : operationDefinitionContext.selectionSet().selection()) {
            if (selectionContext.field().arguments() != null) {
                GraphqlParser.FieldDefinitionContext fieldDefinitionContext = manager.getSubscriptionOperationTypeName()
                        .map(name -> manager.getField(name, selectionContext.field().name().getText())
                                .orElseThrow(() -> new GraphQLErrors(GraphQLErrorType.FIELD_NOT_EXIST.bind(name, selectionContext.field().name().getText())))
                        )
                        .orElseThrow(() -> new GraphQLErrors(GraphQLErrorType.SUBSCRIBE_TYPE_NOT_EXIST));

                String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
                filterSelectionList.computeIfAbsent(fieldTypeName, k -> new CopyOnWriteArrayList<>());
                List<Field> fields = argumentsToFields(fieldTypeName, selectionContext.field().arguments());
                Field.mergeSelection(filterSelectionList.get(fieldTypeName), fields);
            }
        }
    }

    private List<Field> argumentsToFields(String typeName, GraphqlParser.ArgumentsContext argumentsContext) {
        return argumentsContext.argument().stream()
                .filter(argumentContext -> Arrays.stream(EXCLUDE_INPUT).noneMatch(inputName -> inputName.equals(argumentContext.name().getText())))
                .map(argumentContext -> {
                            GraphqlParser.FieldDefinitionContext fieldDefinitionContext = manager.getField(typeName, argumentContext.name().getText())
                                    .orElseThrow(() -> new GraphQLErrors(GraphQLErrorType.FIELD_NOT_EXIST.bind(typeName, argumentContext.name().getText())));
                            String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
                            Field field = new Field(argumentContext.name().getText());
                            if (manager.isObject(fieldTypeName) && argumentContext.valueWithVariable().objectValueWithVariable() != null) {
                                field.setFields(objectValueToFields(fieldTypeName, argumentContext.valueWithVariable().objectValueWithVariable()));
                            }
                            return field;
                        }
                )
                .collect(Collectors.toList());
    }

    private List<Field> objectValueToFields(String typeName, GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {
        return objectValueWithVariableContext.objectFieldWithVariable().stream()
                .filter(objectFieldWithVariableContext -> Arrays.stream(EXCLUDE_INPUT).noneMatch(inputName -> inputName.equals(objectFieldWithVariableContext.name().getText())))
                .map(objectFieldWithVariableContext -> {
                            GraphqlParser.FieldDefinitionContext fieldDefinitionContext = manager.getField(typeName, objectFieldWithVariableContext.name().getText())
                                    .orElseThrow(() -> new GraphQLErrors(GraphQLErrorType.FIELD_NOT_EXIST.bind(typeName, objectFieldWithVariableContext.name().getText())));
                            String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
                            Field field = new Field(objectFieldWithVariableContext.name().getText());
                            if (manager.isObject(fieldTypeName) && objectFieldWithVariableContext.valueWithVariable().objectValueWithVariable() != null) {
                                field.setFields(objectValueToFields(fieldTypeName, objectFieldWithVariableContext.valueWithVariable().objectValueWithVariable()));
                            }
                            return field;
                        }
                )
                .collect(Collectors.toList());
    }

    public abstract Flux<JsonValue> subscriptionOperation(OperationHandler operationHandler, GraphqlParser.OperationDefinitionContext operationDefinitionContext, String token, String operationId);

    public abstract Mono<JsonValue> sendMutation(GraphqlParser.OperationDefinitionContext operationDefinitionContext, Operation operation, JsonValue jsonValue);
}
