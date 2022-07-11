package io.graphoenix.graphql.builder.event;

import com.google.auto.service.AutoService;
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.graphql.builder.schema.DocumentBuilder;
import io.graphoenix.spi.handler.ScopeEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import org.tinylog.Logger;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

@Initialized(ApplicationScoped.class)
@Priority(0)
@AutoService(ScopeEvent.class)
public class DocumentInitializedEvent implements ScopeEvent {

    private final DocumentBuilder documentBuilder;

    public DocumentInitializedEvent() {
        this.documentBuilder = BeanContext.get(DocumentBuilder.class);
    }

    @Override
    public void fire(Map<String, Object> context) {
        try {
            documentBuilder.startupManager();
            Logger.info("document initialized success");
        } catch (IOException | URISyntaxException e) {
            Logger.error(e);
            Logger.info("document initialized failed");
        }
    }
}
