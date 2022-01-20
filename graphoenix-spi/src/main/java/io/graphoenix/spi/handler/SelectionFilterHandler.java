package io.graphoenix.spi.handler;

import com.google.gson.JsonElement;
import graphql.parser.antlr.GraphqlParser;

import java.util.Collection;
import java.util.function.BiFunction;

public interface SelectionFilterHandler {

    <T> BiFunction<T, GraphqlParser.SelectionSetContext, JsonElement> getFilter(Class<T> beanClass);

    <T> BiFunction<Collection<T>, GraphqlParser.SelectionSetContext, JsonElement> getListFilter(Class<T> beanClass);
}
