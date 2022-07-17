package io.graphoenix.json;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.json.spi.JsonProvider;

public class JsonbBuilderImpl implements JsonbBuilder {
    @Override
    public JsonbBuilder withConfig(JsonbConfig config) {
        return this;
    }

    @Override
    public JsonbBuilder withProvider(JsonProvider jsonpProvider) {
        return this;
    }

    @Override
    public Jsonb build() {
        return new JsonbImpl();
    }
}
