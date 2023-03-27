package io.graphoenix.core.handler;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.error.GraphQLErrorType;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;
import jakarta.json.spi.JsonProvider;

import java.util.Collections;
import java.util.Optional;

import static io.graphoenix.core.error.GraphQLErrorType.CONNECTION_AGG_FIELD_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.CONNECTION_FIELD_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.CONNECTION_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.OBJECT_SELECTION_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.TYPE_ID_FIELD_NOT_EXIST;
import static io.graphoenix.core.utils.DocumentUtil.DOCUMENT_UTIL;
import static io.graphoenix.spi.constant.Hammurabi.AFTER_INPUT_NAME;
import static io.graphoenix.spi.constant.Hammurabi.BEFORE_INPUT_NAME;
import static io.graphoenix.spi.constant.Hammurabi.CONNECTION_DIRECTIVE_NAME;
import static io.graphoenix.spi.constant.Hammurabi.CONNECTION_SUFFIX;
import static io.graphoenix.spi.constant.Hammurabi.CURSOR_DIRECTIVE_NAME;
import static io.graphoenix.spi.constant.Hammurabi.FIRST_INPUT_NAME;
import static io.graphoenix.spi.constant.Hammurabi.LAST_INPUT_NAME;
import static jakarta.json.JsonValue.EMPTY_JSON_OBJECT;
import static jakarta.json.JsonValue.NULL;

@ApplicationScoped
public class ConnectionBuilder {

    private final IGraphQLDocumentManager manager;
    private final JsonProvider jsonProvider;

    @Inject
    public ConnectionBuilder(IGraphQLDocumentManager manager, JsonProvider jsonProvider) {
        this.manager = manager;
        this.jsonProvider = jsonProvider;
    }

    public JsonValue build(JsonValue jsonValue, String typeName, GraphqlParser.SelectionContext selectionContext) {
        if (jsonValue == null || jsonValue.getValueType().equals(JsonValue.ValueType.NULL)) {
            return NULL;
        }
        if (jsonValue.asJsonObject().isEmpty()) {
            return EMPTY_JSON_OBJECT;
        }
        JsonObjectBuilder connectionObjectBuilder = jsonProvider.createObjectBuilder();
        if (selectionContext.field().selectionSet() != null && selectionContext.field().selectionSet().selection().size() > 0) {
            GraphqlParser.FieldDefinitionContext connectionFieldDefinitionContext = manager.getField(typeName, selectionContext.field().name().getText())
                    .orElseThrow(() -> new GraphQLErrors(GraphQLErrorType.FIELD_NOT_EXIST.bind(typeName, selectionContext.field().name().getText())));
            Optional<GraphqlParser.DirectiveContext> connectionDirectiveContext = connectionFieldDefinitionContext.directives().directive().stream()
                    .filter(directiveContext -> directiveContext.name().getText().equals(CONNECTION_DIRECTIVE_NAME))
                    .findFirst();

            if (connectionDirectiveContext.isPresent()) {
                Optional<String> connectionFieldName = connectionDirectiveContext.get().arguments().argument().stream()
                        .filter(argumentContext -> argumentContext.name().getText().equals("field"))
                        .filter(argumentContext -> argumentContext.valueWithVariable().StringValue() != null)
                        .findFirst()
                        .map(argumentContext -> DOCUMENT_UTIL.getStringValue(argumentContext.valueWithVariable().StringValue()));

                Optional<String> connectionAggFieldName = connectionDirectiveContext.get().arguments().argument().stream()
                        .filter(argumentContext -> argumentContext.name().getText().equals("agg"))
                        .filter(argumentContext -> argumentContext.valueWithVariable().StringValue() != null)
                        .findFirst()
                        .map(argumentContext -> DOCUMENT_UTIL.getStringValue(argumentContext.valueWithVariable().StringValue()));

                if (connectionFieldName.isPresent() && connectionAggFieldName.isPresent() && jsonValue.asJsonObject().get(connectionFieldName.get()) != null && !jsonValue.asJsonObject().isNull(connectionFieldName.get())) {
                    String fieldName = selectionContext.field().name().getText().substring(0, selectionContext.field().name().getText().length() - CONNECTION_SUFFIX.length());
                    GraphqlParser.FieldDefinitionContext fieldDefinitionContext = manager.getField(typeName, fieldName)
                            .orElseThrow(() -> new GraphQLErrors(GraphQLErrorType.FIELD_NOT_EXIST.bind(typeName, fieldName)));
                    String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
                    String cursorFieldName = manager.getFieldByDirective(fieldTypeName, CURSOR_DIRECTIVE_NAME)
                            .findFirst()
                            .or(() -> manager.getObjectTypeIDFieldDefinition(fieldTypeName))
                            .orElseThrow(() -> new GraphQLErrors(TYPE_ID_FIELD_NOT_EXIST.bind(fieldTypeName)))
                            .name()
                            .getText();

                    for (GraphqlParser.SelectionContext connectionSelectionContext : selectionContext.field().selectionSet().selection()) {
                        JsonArray nodeArray = jsonValue.asJsonObject().get(connectionFieldName.get()).asJsonArray();
                        int limit;
                        boolean isLast;
                        boolean isBefore;
                        boolean isAfter;
                        if (selectionContext.field().arguments() != null && selectionContext.field().arguments().argument().size() > 0) {
                            limit = selectionContext.field().arguments().argument().stream()
                                    .filter(argumentContext -> argumentContext.name().getText().equals(FIRST_INPUT_NAME))
                                    .findFirst()
                                    .map(argumentContext -> Integer.parseInt(argumentContext.valueWithVariable().IntValue().getText()))
                                    .orElseGet(() ->
                                            selectionContext.field().arguments().argument().stream()
                                                    .filter(argumentContext -> argumentContext.name().getText().equals(LAST_INPUT_NAME))
                                                    .findFirst()
                                                    .map(argumentContext -> Integer.parseInt(argumentContext.valueWithVariable().IntValue().getText()))
                                                    .orElse(nodeArray.size())
                                    );
                            isLast = selectionContext.field().arguments().argument().stream().anyMatch(argumentContext -> argumentContext.name().getText().equals(LAST_INPUT_NAME));
                            isBefore = selectionContext.field().arguments().argument().stream().anyMatch(argumentContext -> argumentContext.name().getText().equals(BEFORE_INPUT_NAME));
                            isAfter = selectionContext.field().arguments().argument().stream().anyMatch(argumentContext -> argumentContext.name().getText().equals(AFTER_INPUT_NAME));
                        } else {
                            limit = nodeArray.size();
                            isLast = false;
                            isBefore = false;
                            isAfter = false;
                        }
                        boolean isFirst = !isLast;

                        switch (connectionSelectionContext.field().name().getText()) {
                            case "totalCount":
                                String idFieldName = manager.getObjectTypeIDFieldName(fieldTypeName).orElseThrow(() -> new GraphQLErrors(TYPE_ID_FIELD_NOT_EXIST.bind(fieldTypeName)));
                                connectionObjectBuilder.add("totalCount", jsonValue.asJsonObject().get(connectionAggFieldName.get()).asJsonObject().get(idFieldName.concat("Count")));
                                break;
                            case "edges":
                                if (connectionSelectionContext.field().selectionSet() == null || connectionSelectionContext.field().selectionSet().selection().size() == 0) {
                                    throw new GraphQLErrors(OBJECT_SELECTION_NOT_EXIST.bind(connectionSelectionContext.field().getText()));
                                }
                                JsonArrayBuilder edgesBuilder = jsonProvider.createArrayBuilder();
                                int index = 0;
                                for (JsonValue node : nodeArray) {
                                    if (index < limit) {
                                        JsonObjectBuilder edge = jsonProvider.createObjectBuilder();
                                        for (GraphqlParser.SelectionContext edgesSelectionContext : connectionSelectionContext.field().selectionSet().selection()) {
                                            if (edgesSelectionContext.field().name().getText().equals("cursor")) {
                                                edge.add("cursor", node.asJsonObject().get(cursorFieldName));
                                            } else if (edgesSelectionContext.field().name().getText().equals("node")) {
                                                edge.add("node", node.asJsonObject());
                                            }
                                        }
                                        edgesBuilder.add(edge);
                                    }
                                    index++;
                                }
                                JsonArray edges = edgesBuilder.build();
                                if (isLast) {
                                    Collections.reverse(edges);
                                }
                                connectionObjectBuilder.add("edges", edges);
                                break;
                            case "pageInfo":
                                if (connectionSelectionContext.field().selectionSet() == null || connectionSelectionContext.field().selectionSet().selection().size() == 0) {
                                    throw new GraphQLErrors(OBJECT_SELECTION_NOT_EXIST.bind(connectionSelectionContext.field().getText()));
                                }
                                JsonObjectBuilder pageInfoBuilder = jsonProvider.createObjectBuilder();
                                for (GraphqlParser.SelectionContext pageInfoSelectionContext : connectionSelectionContext.field().selectionSet().selection()) {
                                    switch (pageInfoSelectionContext.field().name().getText()) {
                                        case "hasNextPage":
                                            if (isFirst) {
                                                pageInfoBuilder.add("hasNextPage", nodeArray != null && limit < nodeArray.size());
                                            } else {
                                                pageInfoBuilder.add("hasNextPage", isBefore);
                                            }
                                            break;
                                        case "hasPreviousPage":
                                            if (isLast) {
                                                pageInfoBuilder.add("hasPreviousPage", nodeArray != null && limit < nodeArray.size());
                                            } else {
                                                pageInfoBuilder.add("hasPreviousPage", isAfter);
                                            }
                                            break;
                                        case "startCursor":
                                            if (nodeArray != null && nodeArray.size() > 0) {
                                                if (limit < nodeArray.size()) {
                                                    if (isFirst) {
                                                        pageInfoBuilder.add("startCursor", nodeArray.get(0).asJsonObject().get(cursorFieldName));
                                                    } else {
                                                        pageInfoBuilder.add("startCursor", nodeArray.get(nodeArray.size() - 2).asJsonObject().get(cursorFieldName));
                                                    }
                                                } else {
                                                    if (isFirst) {
                                                        pageInfoBuilder.add("startCursor", nodeArray.get(0).asJsonObject().get(cursorFieldName));
                                                    } else {
                                                        pageInfoBuilder.add("startCursor", nodeArray.get(nodeArray.size() - 1).asJsonObject().get(cursorFieldName));
                                                    }
                                                }
                                            } else {
                                                pageInfoBuilder.add("startCursor", NULL);
                                            }
                                            break;
                                        case "endCursor":
                                            if (nodeArray != null && nodeArray.size() > 0) {
                                                if (limit < nodeArray.size()) {
                                                    if (isFirst) {
                                                        pageInfoBuilder.add("endCursor", nodeArray.get(nodeArray.size() - 2).asJsonObject().get(cursorFieldName));
                                                    } else {
                                                        pageInfoBuilder.add("endCursor", nodeArray.get(0).asJsonObject().get(cursorFieldName));
                                                    }
                                                } else {
                                                    if (isFirst) {
                                                        pageInfoBuilder.add("endCursor", nodeArray.get(nodeArray.size() - 1).asJsonObject().get(cursorFieldName));
                                                    } else {
                                                        pageInfoBuilder.add("endCursor", nodeArray.get(0).asJsonObject().get(cursorFieldName));
                                                    }
                                                }
                                            } else {
                                                pageInfoBuilder.add("endCursor", NULL);
                                            }
                                            break;
                                    }
                                }
                                connectionObjectBuilder.add("pageInfo", pageInfoBuilder);
                                break;
                        }
                    }
                } else {
                    if (connectionFieldName.isEmpty()) {
                        throw new GraphQLErrors(CONNECTION_FIELD_NOT_EXIST.bind(connectionFieldDefinitionContext.name().getText()));
                    } else {
                        throw new GraphQLErrors(CONNECTION_AGG_FIELD_NOT_EXIST.bind(connectionFieldDefinitionContext.name().getText()));
                    }
                }
            } else {
                throw new GraphQLErrors(CONNECTION_NOT_EXIST.bind(connectionFieldDefinitionContext.name().getText()));
            }
        }
        return connectionObjectBuilder.build();
    }
}
