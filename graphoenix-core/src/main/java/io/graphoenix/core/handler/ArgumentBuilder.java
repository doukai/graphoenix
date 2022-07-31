package io.graphoenix.core.handler;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.error.GraphQLErrorType;
import io.graphoenix.core.error.GraphQLErrors;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.bind.Jsonb;

import java.util.stream.Collectors;

@ApplicationScoped
public class ArgumentBuilder {

    private final Jsonb jsonb;

    @Inject
    public ArgumentBuilder(Jsonb jsonb) {
        this.jsonb = jsonb;
    }

    public  <T> T getArgument(GraphqlParser.SelectionContext selectionContext, String name, Class<T> beanClass) {
        return selectionContext.field().arguments().argument().stream()
                .filter(argumentContext -> argumentContext.name().getText().equals(name))
                .findFirst()
                .map(argumentContext -> jsonb.fromJson(valueWithVariableToJsonString(argumentContext.valueWithVariable()), beanClass))
                .orElseThrow(() -> new GraphQLErrors(GraphQLErrorType.SELECTION_ARGUMENT_NOT_EXIST.bind(name, selectionContext.field().name().getText())));
    }

    protected String valueWithVariableToJsonString(GraphqlParser.ValueWithVariableContext valueWithVariableContext) {
        if (valueWithVariableContext.objectValueWithVariable() != null) {
            return valueWithVariableContext.objectValueWithVariable().objectFieldWithVariable().stream()
                    .map(objectFieldWithVariableContext -> valueWithVariableToJsonString(objectFieldWithVariableContext.valueWithVariable()))
                    .collect(Collectors.joining(","));
        } else {
            return valueWithVariableContext.getText();
        }
    }
}
