package io.graphoenix.core.produces;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.spi.JsonbProvider;
import jakarta.json.spi.JsonProvider;

@ApplicationScoped
public class JsonProducer {

    @Produces
    @ApplicationScoped
    public JsonProvider jsonProvider() {
        return JsonProvider.provider();
    }

    @Produces
    @ApplicationScoped
    public JsonbProvider jsonbProvider() {
        return JsonbProvider.provider();
    }

    @Produces
    @ApplicationScoped
    public Jsonb jsonb() {
        return jsonbProvider().create().build();
    }
}
