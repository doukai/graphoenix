package io.graphoenix.grpc.client;

import java.util.Collection;
import java.util.stream.Collectors;

public class GrpcBaseInvokeHandler {

    public String getListArguments(Collection<String> argumentList) {
        return "list: [".concat(argumentList.stream().map(argument -> "{".concat(argument).concat("}")).collect(Collectors.joining(", "))).concat("]");
    }
}
