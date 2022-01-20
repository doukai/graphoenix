package io.graphoenix.core.handler;

import com.google.gson.GsonBuilder;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.spi.handler.QueryHandler;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.function.BiFunction;

import static io.graphoenix.core.utils.DocumentUtil.DOCUMENT_UTIL;

public abstract class BaseQueryHandler implements QueryHandler {

    private final GsonBuilder gsonBuilder = new GsonBuilder();

    private Map<String, BiFunction<String, Map<String, String>, Mono<String>>> invokeFunctions;

    @Override
    public BiFunction<String, Map<String, String>, Mono<String>> getInvokeMethod(String name) {
        return invokeFunctions.get(name);
    }

    protected void put(String name, BiFunction<String, Map<String, String>, Mono<String>> biFunction) {
        invokeFunctions.put(name, biFunction);
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
