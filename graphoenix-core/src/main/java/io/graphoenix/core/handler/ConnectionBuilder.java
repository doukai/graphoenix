package io.graphoenix.core.handler;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.error.GraphQLErrorType;
import io.graphoenix.core.error.GraphQLProblem;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Optional;

import static io.graphoenix.core.error.GraphQLErrorType.*;
import static io.graphoenix.core.utils.DocumentUtil.DOCUMENT_UTIL;
import static io.graphoenix.spi.constant.Hammurabi.FIRST_INPUT_NAME;
import static io.graphoenix.spi.constant.Hammurabi.LAST_INPUT_NAME;

@ApplicationScoped
public class ConnectionBuilder {

    private final IGraphQLDocumentManager manager;

    @Inject
    public ConnectionBuilder(IGraphQLDocumentManager manager) {
        this.manager = manager;
    }

    public JsonElement build(JsonElement jsonElement, String typeName, String idFieldName, String cursorFieldName, GraphqlParser.SelectionContext selectionContext) {
        JsonObject connectionObject = new JsonObject();
        if (selectionContext.field().selectionSet() != null && selectionContext.field().selectionSet().selection().size() > 0) {
            GraphqlParser.FieldDefinitionContext fieldDefinitionContext = manager.getField(typeName, selectionContext.field().name().getText())
                    .orElseThrow(() -> new GraphQLProblem(GraphQLErrorType.FIELD_NOT_EXIST.bind(typeName, selectionContext.field().name().getText())));

            Optional<GraphqlParser.DirectiveContext> connection = fieldDefinitionContext.directives().directive().stream()
                    .filter(directiveContext -> directiveContext.name().getText().equals("connection"))
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
                    for (GraphqlParser.SelectionContext connectionSelectionContext : selectionContext.field().selectionSet().selection()) {
                        switch (connectionSelectionContext.field().name().getText()) {
                            case "totalCount":
                                connectionObject.add("totalCount", jsonElement.getAsJsonObject().get(connectionAggFieldName.get()).getAsJsonObject().get(idFieldName.concat("Count")));
                                break;
                            case "edges":
                                JsonArray edges = new JsonArray();
                                if (connectionSelectionContext.field().selectionSet() != null && connectionSelectionContext.field().selectionSet().selection().size() > 0) {
                                    JsonArray nodeArray = jsonElement.getAsJsonObject().get(connectionFieldName.get()).getAsJsonArray();
                                    int index = 0;
                                    for (GraphqlParser.SelectionContext edgesSelectionContext : connectionSelectionContext.field().selectionSet().selection()) {
                                        if (index < nodeArray.size() - 1) {
                                            JsonObject edge = new JsonObject();
                                            if (edgesSelectionContext.field().name().getText().equals("cursor")) {
                                                edge.add("cursor", nodeArray.get(index).getAsJsonObject().get(cursorFieldName));
                                            } else if (edgesSelectionContext.field().name().getText().equals("node")) {
                                                edge.add("node", nodeArray.get(index).getAsJsonObject());
                                            }
                                            edges.add(edge);
                                        }
                                    }
                                }
                                connectionObject.add("edges", edges);
                                break;
                            case "pageInfo":
                                GraphqlParser.ArgumentContext limitArgument = selectionContext.field().arguments().argument().stream()
                                        .filter(argumentContext -> argumentContext.name().getText().equals(FIRST_INPUT_NAME))
                                        .findFirst()
                                        .orElseGet(() ->
                                                selectionContext.field().arguments().argument().stream()
                                                        .filter(argumentContext -> argumentContext.getText().equals(LAST_INPUT_NAME))
                                                        .findFirst()
                                                        .orElseThrow()
                                        );
                                int limit = Integer.parseInt(limitArgument.valueWithVariable().IntValue().getText());
                                JsonObject pageInfo = new JsonObject();
                                JsonArray nodeArray = jsonElement.getAsJsonObject().get(connectionFieldName.get()).getAsJsonArray();

                                for (GraphqlParser.SelectionContext pageInfoSelectionContext : connectionSelectionContext.field().selectionSet().selection()) {
                                    switch (pageInfoSelectionContext.field().name().getText()) {
                                        case "hasNextPage":
                                            pageInfo.addProperty("hasNextPage", nodeArray != null && limit < nodeArray.size());
                                            break;
                                        case "hasPreviousPage":
                                            pageInfo.addProperty("hasPreviousPage", nodeArray != null && limit < nodeArray.size());
                                            break;
                                        case "startCursor":
                                            if (nodeArray != null && nodeArray.size() > 2) {
                                                pageInfo.add("startCursor", nodeArray.get(nodeArray.size() - 2).getAsJsonObject().get(cursorFieldName));
                                            } else {
                                                pageInfo.add("startCursor", JsonNull.INSTANCE);
                                            }
                                            break;
                                        case "endCursor":
                                            if (nodeArray != null && nodeArray.size() > 2) {
                                                pageInfo.add("endCursor", nodeArray.get(nodeArray.size() - 2).getAsJsonObject().get(cursorFieldName));
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
                        throw new GraphQLProblem(CONNECTION_FIELD_NOT_EXIST.bind(fieldDefinitionContext.name().getText()));
                    } else {
                        throw new GraphQLProblem(CONNECTION_AGG_FIELD_NOT_EXIST.bind(fieldDefinitionContext.name().getText()));
                    }
                }
            }
            throw new GraphQLProblem(CONNECTION_NOT_EXIST.bind(fieldDefinitionContext.name().getText()));
        }
        return connectionObject;
    }
}
