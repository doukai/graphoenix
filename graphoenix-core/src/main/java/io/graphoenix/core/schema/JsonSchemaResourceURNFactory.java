package io.graphoenix.core.schema;

import com.networknt.schema.urn.URNFactory;
import jakarta.enterprise.context.ApplicationScoped;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

@ApplicationScoped
public class JsonSchemaResourceURNFactory implements URNFactory {

    @Override
    public URI create(String urn) {
        try {
            return Objects.requireNonNull(getClass().getClassLoader().getResource("META-INF/schema/".concat(urn).concat(".json"))).toURI();
        } catch (URISyntaxException e) {
            return null;
        }
    }
}
