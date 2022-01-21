package io.graphoenix.core.handler;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.spi.handler.QueryHandler;
import io.vavr.Function3;

import java.util.HashMap;
import java.util.Map;

import static io.graphoenix.core.utils.DocumentUtil.DOCUMENT_UTIL;

public abstract class BaseQueryHandler implements QueryHandler {

    private final GsonBuilder gsonBuilder;

    private final Map<String, Function3<JsonElement, String, Map<String, String>, JsonElement>> operationHandlers;

    public BaseQueryHandler() {
        this.gsonBuilder = new GsonBuilder();
        this.operationHandlers = new HashMap<>();
    }

    @Override
    public Function3<JsonElement, String, Map<String, String>, JsonElement> getOperationHandler(String name) {
        return operationHandlers.get(name);
    }

    protected void put(String name, Function3<JsonElement, String, Map<String, String>, JsonElement> function3) {
        operationHandlers.put(name, function3);
    }

    protected GraphqlParser.SelectionContext getSelectionContext(String graphQL, String name) {
        return DOCUMENT_UTIL.graphqlToOperation(graphQL).selectionSet().selection().stream()
                .filter(selectionContext -> selectionContext.field().name().getText().equals(name))
                .findFirst()
                .orElseThrow();
    }

    protected <T> T getArgument(GraphqlParser.SelectionContext selectionContext, String name, Class<T> beanClass) {
        return selectionContext.field().arguments().argument().stream()
                .filter(argumentContext -> argumentContext.name().getText().equals(name))
                .findFirst()
                .map(argumentContext -> gsonBuilder.create().fromJson(argumentContext.valueWithVariable().getText(), beanClass))
                .orElseThrow();
    }
}
