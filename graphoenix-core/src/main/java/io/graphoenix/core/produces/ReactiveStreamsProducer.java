package io.graphoenix.core.produces;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreamsFactory;
import org.eclipse.microprofile.reactive.streams.operators.spi.ReactiveStreamsFactoryResolver;

@ApplicationScoped
public class ReactiveStreamsProducer {

    @Produces
    @ApplicationScoped
    public ReactiveStreamsFactory reactiveStreamsFactory() {
        return ReactiveStreamsFactoryResolver.instance();
    }
}
