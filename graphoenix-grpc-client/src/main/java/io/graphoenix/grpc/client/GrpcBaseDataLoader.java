package io.graphoenix.grpc.client;

import graphql.parser.antlr.GraphqlParser;

import java.util.Collection;
import java.util.stream.Collectors;

import static io.graphoenix.spi.constant.Hammurabi.LIST_INPUT_NAME;

public class GrpcBaseDataLoader {

    public String valueWithVariableToString(GraphqlParser.ValueWithVariableContext valueWithVariableContext) {
        if (valueWithVariableContext.objectValueWithVariable() != null) {
            return "{".concat(
                    valueWithVariableContext.objectValueWithVariable().objectFieldWithVariable().stream()
                            .map(objectFieldWithVariableContext ->
                                    objectFieldWithVariableContext.name().getText()
                                            .concat(": ")
                                            .concat(valueWithVariableToString(objectFieldWithVariableContext.valueWithVariable()))
                            )
                            .collect(Collectors.joining(" "))
            ).concat("}");
        } else if (valueWithVariableContext.arrayValueWithVariable() != null) {
            return "[".concat(
                    valueWithVariableContext.arrayValueWithVariable().valueWithVariable().stream()
                            .map(this::valueWithVariableToString)
                            .collect(Collectors.joining(", "))
            ).concat("]");
        } else {
            return valueWithVariableContext.getText();
        }
    }

    public String getListArguments(Collection<String> argumentList) {
        return LIST_INPUT_NAME.concat(": [".concat(String.join(", ", argumentList)).concat("]"));
    }
}
