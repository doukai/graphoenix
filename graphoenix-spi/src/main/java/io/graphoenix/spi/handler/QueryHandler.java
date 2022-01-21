package io.graphoenix.spi.handler;

import com.google.gson.JsonElement;
import io.vavr.Function3;

import java.util.Map;

public interface QueryHandler {

    Function3<JsonElement, String, Map<String, String>, JsonElement> getOperationHandler(String name);
}
