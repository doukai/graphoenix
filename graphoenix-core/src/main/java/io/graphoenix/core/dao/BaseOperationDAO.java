package io.graphoenix.core.dao;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.spi.dao.OperationDAO;
import io.vavr.CheckedFunction0;
import jakarta.json.JsonObject;
import jakarta.json.bind.Jsonb;
import jakarta.json.spi.JsonProvider;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreamsFactory;
import org.reactivestreams.Publisher;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Type;

public abstract class BaseOperationDAO implements OperationDAO {

    private final JsonProvider jsonProvider;
    private final Jsonb jsonb;
    private final ReactiveStreamsFactory reactiveStreamsFactory;

    public BaseOperationDAO() {
        this.jsonProvider = BeanContext.get(JsonProvider.class);
        this.jsonb = BeanContext.get(Jsonb.class);
        this.reactiveStreamsFactory = BeanContext.get(ReactiveStreamsFactory.class);
    }

    protected static <T> String fileToString(Class<T> beanClass, String fileName) {
        InputStream resourceAsStream = beanClass.getResourceAsStream(fileName);
        assert resourceAsStream != null;
        return CheckedFunction0.of(() -> CharStreams.toString(new InputStreamReader(resourceAsStream, Charsets.UTF_8))).unchecked().get();
    }

    protected <T> T jsonToType(String jsonString, Class<T> beanClass) {
        JsonObject jsonObject = jsonProvider.createReader(new StringReader(jsonString)).readObject();
        String key = (String) jsonObject.keySet().toArray()[0];
        StringWriter stringWriter = new StringWriter();
        jsonProvider.createWriter(stringWriter).write(jsonObject.get(key));
        return jsonb.fromJson(stringWriter.toString(), beanClass);
    }

    protected <T> T jsonToType(String jsonString, Type type) {
        JsonObject jsonObject = jsonProvider.createReader(new StringReader(jsonString)).readObject();
        String key = (String) jsonObject.keySet().toArray()[0];
        StringWriter stringWriter = new StringWriter();
        jsonProvider.createWriter(stringWriter).write(jsonObject.get(key));
        return jsonb.fromJson(stringWriter.toString(), type);
    }

    protected <T> PublisherBuilder<T> toBuilder(Publisher<T> publisher) {
        return reactiveStreamsFactory.fromPublisher(publisher);
    }
}
