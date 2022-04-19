package io.graphoenix.core.handler;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.error.GraphQLErrorType;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
import static io.graphoenix.spi.constant.Hammurabi.FIRST_INPUT_NAME;
import static io.graphoenix.spi.constant.Hammurabi.LAST_INPUT_NAME;

@ApplicationScoped
public class ConnectionBuilder {

    private final IGraphQLDocumentManager manager;

    @Inject
    public ConnectionBuilder(IGraphQLDocumentManager manager) {
        this.manager = manager;
    }

    public JsonElement build(JsonElement jsonElement, String typeName, GraphqlParser.SelectionContext selectionContext) {
        JsonObject connectionObject = new JsonObject();
        if (selectionContext.field().selectionSet() != null && selectionContext.field().selectionSet().selection().size() > 0) {
            GraphqlParser.FieldDefinitionContext connectionFieldDefinitionContext = manager.getField(typeName, selectionContext.field().name().getText())
                    .orElseThrow(() -> new GraphQLErrors(GraphQLErrorType.FIELD_NOT_EXIST.bind(typeName, selectionContext.field().name().getText())));
            Optional<GraphqlParser.DirectiveContext> connection = connectionFieldDefinitionContext.directives().directive().stream()
                    .filter(directiveContext -> directiveContext.name().getText().equals(CONNECTION_DIRECTIVE_NAME))
                    .findFirst();

            if (connection.isPresent()) {
                Optional<String> connectionFieldName = connection.get().arguments().argument().stream()
                        .filter(argumentContext -> argumentContext.name().getText().equals("field"))
                        .filter(argumentContext -> argumentContext.valueWithVariable().StringValue() != null)
                        .findFirst()
                        .map(argumentContext -> DOCUMENT_UTIL.getStringValue(argumentContext.valueWithVariable().StringValue()));

                Optional<String> connectionAggFieldName = connection.get().arguments().argument().stream()
                        .filter(argumentContext -> argumentContext.name().getText().equals("agg"))
                        .filter(argumentContext -> argumentContext.valueWithVariable().StringValue() != null)
                        .findFirst()
                        .map(argumentContext -> DOCUMENT_UTIL.getStringValue(argumentContext.valueWithVariable().StringValue()));

                if (connectionFieldName.isPresent() && connectionAggFieldName.isPresent()) {
                    String fieldName = selectionContext.field().name().getText().substring(0, selectionContext.field().name().getText().length() - CONNECTION_SUFFIX.length());
                    GraphqlParser.FieldDefinitionContext fieldDefinitionContext = manager.getField(typeName, fieldName)
                            .orElseThrow(() -> new GraphQLErrors(GraphQLErrorType.FIELD_NOT_EXIST.bind(typeName, fieldName)));
                    String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
                    String cursorFieldName = manager.getFieldByDirective(fieldTypeName, "cursor")
                            .findFirst()
                            .or(() -> manager.getObjectTypeIDFieldDefinition(fieldTypeName))
                            .orElseThrow(() -> new GraphQLErrors(TYPE_ID_FIELD_NOT_EXIST.bind(fieldTypeName)))
                            .name()
                            .getText();

                    for (GraphqlParser.SelectionContext connectionSelectionContext : selectionContext.field().selectionSet().selection()) {
                        JsonArray nodeArray = jsonElement.getAsJsonObject().get(connectionFieldName.get()).getAsJsonArray();
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
                                connectionObject.add("totalCount", jsonElement.getAsJsonObject().get(connectionAggFieldName.get()).getAsJsonObject().get(idFieldName.concat("Count")));
                                break;
                            case "edges":
                                if (connectionSelectionContext.field().selectionSet() == null || connectionSelectionContext.field().selectionSet().selection().size() == 0) {
                                    throw new GraphQLErrors(OBJECT_SELECTION_NOT_EXIST.bind(connectionSelectionContext.field().getText()));
                                }
                                JsonArray edges = new JsonArray();
                                List<JsonElement> jsonElementList = new ArrayList<>();
                                int index = 0;
                                for (JsonElement node : nodeArray) {
                                    if (index < limit) {
                                        JsonObject edge = new JsonObject();
                                        for (GraphqlParser.SelectionContext edgesSelectionContext : connectionSelectionContext.field().selectionSet().selection()) {
                                            if (edgesSelectionContext.field().name().getText().equals("cursor")) {
                                                edge.add("cursor", node.getAsJsonObject().get(cursorFieldName));
                                            } else if (edgesSelectionContext.field().name().getText().equals("node")) {
                                                edge.add("node", node.getAsJsonObject());
                                            }
                                        }
                                        jsonElementList.add(edge);
                                    }
                                    index++;
                                }
                                if (isLast) {
                                    Collections.reverse(jsonElementList);
                                }
                                jsonElementList.forEach(edges::add);
                                connectionObject.add("edges", edges);
                                break;
                            case "pageInfo":
                                if (connectionSelectionContext.field().selectionSet() == null || connectionSelectionContext.field().selectionSet().selection().size() == 0) {
                                    throw new GraphQLErrors(OBJECT_SELECTION_NOT_EXIST.bind(connectionSelectionContext.field().getText()));
                                }
                                JsonObject pageInfo = new JsonObject();
                                for (GraphqlParser.SelectionContext pageInfoSelectionContext : connectionSelectionContext.field().selectionSet().selection()) {
                                    switch (pageInfoSelectionContext.field().name().getText()) {
                                        case "hasNextPage":
                                            if (isFirst) {
                                                pageInfo.addProperty("hasNextPage", nodeArray != null && limit < nodeArray.size());
                                            } else {
                                                pageInfo.addProperty("hasNextPage", isBefore);
                                            }
                                            break;
                                        case "hasPreviousPage":
                                            if (isLast) {
                                                pageInfo.addProperty("hasPreviousPage", nodeArray != null && limit < nodeArray.size());
                                            } else {
                                                pageInfo.addProperty("hasPreviousPage", isAfter);
                                            }
                                            break;
                                        case "startCursor":
                                            if (nodeArray != null && nodeArray.size() > 0) {
                                                if (limit < nodeArray.size()) {
                                                    if (isFirst) {
                                                        pageInfo.add("startCursor", nodeArray.get(0).getAsJsonObject().get(cursorFieldName));
                                                    } else {
                                                        pageInfo.add("startCursor", nodeArray.get(nodeArray.size() - 2).getAsJsonObject().get(cursorFieldName));
                                                    }
                                                } else {
                                                    if (isFirst) {
                                                        pageInfo.add("startCursor", nodeArray.get(0).getAsJsonObject().get(cursorFieldName));
                                                    } else {
                                                        pageInfo.add("startCursor", nodeArray.get(nodeArray.size() - 1).getAsJsonObject().get(cursorFieldName));
                                                    }
                                                }
                                            } else {
                                                pageInfo.add("startCursor", JsonNull.INSTANCE);
                                            }
                                            break;
                                        case "endCursor":
                                            if (nodeArray != null && nodeArray.size() > 0) {
                                                if (limit < nodeArray.size()) {
                                                    if (isFirst) {
                                                        pageInfo.add("endCursor", nodeArray.get(nodeArray.size() - 2).getAsJsonObject().get(cursorFieldName));
                                                    } else {
                                                        pageInfo.add("endCursor", nodeArray.get(0).getAsJsonObject().get(cursorFieldName));
                                                    }
                                                } else {
                                                    if (isFirst) {
                                                        pageInfo.add("endCursor", nodeArray.get(nodeArray.size() - 1).getAsJsonObject().get(cursorFieldName));
                                                    } else {
                                                        pageInfo.add("endCursor", nodeArray.get(0).getAsJsonObject().get(cursorFieldName));
                                                    }
                                                }
                                            } else {
                                                pageInfo.add("endCursor", JsonNull.INSTANCE);
                                            }
                                            break;
                                    }
                                }
                                connectionObject.add("pageInfo", pageInfo);
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
        return connectionObject;
    }
}
