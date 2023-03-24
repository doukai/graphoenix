package io.graphoenix.core.dao;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.spi.dao.OperationDAO;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreamsFactory;
import org.reactivestreams.Publisher;
import org.tinylog.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public abstract class BaseOperationDAO implements OperationDAO {

    private final ReactiveStreamsFactory reactiveStreamsFactory;

    public BaseOperationDAO() {
        this.reactiveStreamsFactory = BeanContext.get(ReactiveStreamsFactory.class);
    }

    protected static <T> String fileToString(Class<T> beanClass, String fileName) {
        InputStream resourceAsStream = beanClass.getResourceAsStream(fileName);
        assert resourceAsStream != null;
        try {
            return CharStreams.toString(new InputStreamReader(resourceAsStream, Charsets.UTF_8));
        } catch (IOException e) {
            Logger.error(e);
        }
        return null;
    }

    protected <T> PublisherBuilder<T> toBuilder(Publisher<T> publisher) {
        return reactiveStreamsFactory.fromPublisher(publisher);
    }
}
