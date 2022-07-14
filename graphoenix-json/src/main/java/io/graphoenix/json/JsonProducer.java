package io.graphoenix.json;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.json.spi.JsonProvider;

@ApplicationScoped
public class JsonProducer {

    @Produces
    @ApplicationScoped
    public JsonProvider jsonProvider() {
        return JsonProvider.provider();
    }
}
