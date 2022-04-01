package io.graphoenix.core.handler;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import graphql.parser.antlr.GraphqlParser;
import jakarta.enterprise.context.ApplicationScoped;

import static io.graphoenix.spi.constant.Hammurabi.FIRST_INPUT_NAME;
import static io.graphoenix.spi.constant.Hammurabi.LAST_INPUT_NAME;

@ApplicationScoped
public class ConnectionBuilder {

    public JsonElement build(JsonElement jsonElement, String idFieldName, String cursorFieldName, String fieldName, String aggFieldName, GraphqlParser.SelectionContext selectionContext) {
        JsonObject connection = new JsonObject();
        if (selectionContext.field().selectionSet() != null && selectionContext.field().selectionSet().selection().size() > 0) {
            for (GraphqlParser.SelectionContext connectionSelectionContext : selectionContext.field().selectionSet().selection()) {
                if (connectionSelectionContext.field().name().getText().equals("totalCount")) {
                    connection.add("totalCount", jsonElement.getAsJsonObject().get(aggFieldName).getAsJsonObject().get(idFieldName.concat("Count")));
                } else if (connectionSelectionContext.field().name().getText().equals("edges")) {
                    JsonArray edges = new JsonArray();
                    if (connectionSelectionContext.field().selectionSet() != null && connectionSelectionContext.field().selectionSet().selection().size() > 0) {
                        JsonArray nodeArray = jsonElement.getAsJsonObject().get(fieldName).getAsJsonArray();
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
                    connection.add("edges", edges);
                } else if (connectionSelectionContext.field().name().getText().equals("pageInfo")) {
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
                    JsonArray nodeArray = jsonElement.getAsJsonObject().get(fieldName).getAsJsonArray();
                    for (GraphqlParser.SelectionContext pageInfoSelectionContext : connectionSelectionContext.field().selectionSet().selection()) {
                        if (pageInfoSelectionContext.field().name().getText().equals("hasNextPage")) {
                            pageInfo.addProperty("hasNextPage", nodeArray != null && limit < nodeArray.size());
                        } else if (pageInfoSelectionContext.field().name().getText().equals("hasPreviousPage")) {
                            pageInfo.addProperty("hasPreviousPage", nodeArray != null && limit < nodeArray.size());
                        } else if (pageInfoSelectionContext.field().name().getText().equals("startCursor")) {
                            if (nodeArray != null && nodeArray.size() > 2) {
                                pageInfo.add("startCursor", nodeArray.get(nodeArray.size() - 2).getAsJsonObject().get(cursorFieldName));
                            } else {
                                pageInfo.add("startCursor", JsonNull.INSTANCE);
                            }
                        } else if (pageInfoSelectionContext.field().name().getText().equals("endCursor")) {
                            if (nodeArray != null && nodeArray.size() > 2) {
                                pageInfo.add("endCursor", nodeArray.get(nodeArray.size() - 2).getAsJsonObject().get(cursorFieldName));
                            } else {
                                pageInfo.add("endCursor", JsonNull.INSTANCE);
                            }
                        }
                    }
                    connection.add("pageInfo", pageInfo);
                }
            }
        }
        return connection;
    }
}
