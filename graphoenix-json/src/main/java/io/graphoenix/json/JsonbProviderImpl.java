package io.graphoenix.json;

import com.google.auto.service.AutoService;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.spi.JsonbProvider;

@AutoService(JsonbProvider.class)
public class JsonbProviderImpl extends JsonbProvider {
    @Override
    public JsonbBuilder create() {
        return new JsonbBuilderImpl();
    }
}
