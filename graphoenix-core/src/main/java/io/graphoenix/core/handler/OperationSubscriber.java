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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
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

    public Operation buildSubscriptionFilterSelection(Operation operation) {
        for (Field field : operation.getFields()) {
            GraphqlParser.FieldDefinitionContext fieldDefinitionContext = manager.getSubscriptionOperationTypeName()
                    .map(name ->
                            manager.getField(name, field.getName())
                                    .orElseThrow(() -> new GraphQLErrors(GraphQLErrorType.FIELD_NOT_EXIST.bind(name, field.getName())))
                    )
                    .orElseThrow(() -> new GraphQLErrors(GraphQLErrorType.SUBSCRIBE_TYPE_NOT_EXIST));
            String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
            Field.mergeSelection(field.getFields(), filterSelectionList.get(fieldTypeName));
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
                List<Field> fields = argumentsToFields(selectionContext.field().arguments());
                Field.mergeSelection(filterSelectionList.get(fieldTypeName), fields);
            }
        }
    }

    private List<Field> argumentsToFields(GraphqlParser.ArgumentsContext argumentsContext) {
        return argumentsContext.argument().stream()
                .filter(argumentContext -> Arrays.stream(EXCLUDE_INPUT).noneMatch(inputName -> inputName.equals(argumentContext.name().getText())))
                .map(argumentContext -> {
                            Field field = new Field(argumentContext.name().getText());
                            if (argumentContext.valueWithVariable().objectValueWithVariable() != null) {
                                field.setFields(objectValueToFields(argumentContext.valueWithVariable().objectValueWithVariable()));
                            }
                            return field;
                        }
                )
                .collect(Collectors.toList());
    }

    private List<Field> objectValueToFields(GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {
        return objectValueWithVariableContext.objectFieldWithVariable().stream()
                .filter(objectFieldWithVariableContext -> Arrays.stream(EXCLUDE_INPUT).noneMatch(inputName -> inputName.equals(objectFieldWithVariableContext.name().getText())))
                .map(objectFieldWithVariableContext -> {
                            Field field = new Field(objectFieldWithVariableContext.name().getText());
                            if (objectFieldWithVariableContext.valueWithVariable().objectValueWithVariable() != null) {
                                field.setFields(objectValueToFields(objectFieldWithVariableContext.valueWithVariable().objectValueWithVariable()));
                            }
                            return field;
                        }
                )
                .collect(Collectors.toList());
    }

    public abstract Flux<JsonValue> subscriptionOperation(OperationHandler operationHandler, String graphQL, Map<String, JsonValue> variables);

    public abstract Mono<JsonValue> sendMutation(GraphqlParser.OperationDefinitionContext operationDefinitionContext, JsonValue jsonValue);
}
