package io.graphoenix.json;

import com.dslplatform.json.DslJson;
import com.dslplatform.json.JsonWriter;
import com.dslplatform.json.runtime.Settings;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbException;
import org.tinylog.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

public class JsonbImpl implements Jsonb {

    private final DslJson<Object> dslJson;

    public JsonbImpl() {
        this.dslJson = new DslJson<>(Settings.withRuntime().allowArrayFormat(true).includeServiceLoader());
    }

    @Override
    public <T> T fromJson(String str, Class<T> type) throws JsonbException {
        try {
            byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
            return dslJson.deserialize(type, bytes, bytes.length);
        } catch (IOException e) {
            Logger.error(e);
            throw new JsonbException(e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T fromJson(String str, Type runtimeType) throws JsonbException {
        try {
            byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
            return (T) dslJson.deserialize(runtimeType, bytes, bytes.length);
        } catch (IOException e) {
            Logger.error(e);
            throw new JsonbException(e.getMessage(), e);
        }
    }

    @Override
    public <T> T fromJson(Reader reader, Class<T> type) throws JsonbException {
        return null;
    }

    @Override
    public <T> T fromJson(Reader reader, Type runtimeType) throws JsonbException {
        return null;
    }

    @Override
    public <T> T fromJson(InputStream stream, Class<T> type) throws JsonbException {
        try {
            return dslJson.deserialize(type, stream);
        } catch (IOException e) {
            Logger.error(e);
            throw new JsonbException(e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T fromJson(InputStream stream, Type runtimeType) throws JsonbException {
        try {
            return (T) dslJson.deserialize(runtimeType, stream);
        } catch (IOException e) {
            Logger.error(e);
            throw new JsonbException(e.getMessage(), e);
        }
    }

    @Override
    public String toJson(Object object) throws JsonbException {
        try {
            JsonWriter jsonWriter = dslJson.newWriter();
            dslJson.serialize(jsonWriter, object);
            return jsonWriter.toString();
        } catch (IOException e) {
            Logger.error(e);
            throw new JsonbException(e.getMessage(), e);
        }
    }

    @Override
    public String toJson(Object object, Type runtimeType) throws JsonbException {
        JsonWriter jsonWriter = dslJson.newWriter();
        dslJson.serialize(jsonWriter, runtimeType, object);
        return jsonWriter.toString();
    }

    @Override
    public void toJson(Object object, Writer writer) throws JsonbException {

    }

    @Override
    public void toJson(Object object, Type runtimeType, Writer writer) throws JsonbException {

    }

    @Override
    public void toJson(Object object, OutputStream stream) throws JsonbException {
        try {
            dslJson.serialize(object, stream);
        } catch (IOException e) {
            Logger.error(e);
            throw new JsonbException(e.getMessage(), e);
        }
    }

    @Override
    public void toJson(Object object, Type runtimeType, OutputStream stream) throws JsonbException {

    }

    @Override
    public void close() throws Exception {

    }
}
