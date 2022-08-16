package io.graphoenix.grpc.client;

import graphql.parser.antlr.GraphqlParser;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static io.graphoenix.spi.constant.Hammurabi.LIST_INPUT_NAME;

public class GrpcBaseDataLoader {

    public String valueWithVariableToString(GraphqlParser.ValueWithVariableContext valueWithVariableContext) {
        if (valueWithVariableContext.objectValueWithVariable() != null) {
            return objectValueWithVariableToString(valueWithVariableContext.objectValueWithVariable());
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

    public String objectValueWithVariableToString(GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {
        return "{".concat(
                objectValueWithVariableContext.objectFieldWithVariable().stream()
                        .map(objectFieldWithVariableContext ->
                                objectFieldWithVariableContext.name().getText()
                                        .concat(": ")
                                        .concat(valueWithVariableToString(objectFieldWithVariableContext.valueWithVariable()))
                        )
                        .collect(Collectors.joining(" "))
        ).concat("}");
    }

    public List<String> objectValueWithVariableToStringList(GraphqlParser.ArrayValueWithVariableContext arrayValueWithVariableContext) {
        return arrayValueWithVariableContext.valueWithVariable().stream()
                .filter(valueWithVariableContext -> valueWithVariableContext.objectValueWithVariable() != null)
                .map(valueWithVariableContext -> objectValueWithVariableToString(valueWithVariableContext.objectValueWithVariable()))
                .collect(Collectors.toList());
    }

    public String getListArguments(Collection<String> argumentList) {
        return LIST_INPUT_NAME.concat(": [".concat(String.join(", ", argumentList)).concat("]"));
    }
}
