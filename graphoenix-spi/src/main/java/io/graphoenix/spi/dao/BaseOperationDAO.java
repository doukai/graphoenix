package io.graphoenix.spi.dao;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import io.vavr.CheckedFunction0;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public abstract class BaseOperationDAO implements OperationDAO {

    private final GsonBuilder gsonBuilder;

    public BaseOperationDAO() {
        this.gsonBuilder = new GsonBuilder()
                .registerTypeAdapter(
                        LocalDateTime.class,
                        (JsonDeserializer<LocalDateTime>) (json, type, jsonDeserializationContext) ->
                                LocalDateTime.parse(json.getAsJsonPrimitive().getAsString(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                )
                .registerTypeAdapter(
                        LocalDate.class,
                        (JsonDeserializer<LocalDate>) (json, type, jsonDeserializationContext) ->
                                LocalDate.parse(json.getAsJsonPrimitive().getAsString(), DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                )
                .registerTypeAdapter(
                        LocalTime.class,
                        (JsonDeserializer<LocalTime>) (json, type, jsonDeserializationContext) ->
                                LocalTime.parse(json.getAsJsonPrimitive().getAsString(), DateTimeFormatter.ofPattern("HH:mm:ss"))
                );
    }

    protected static <T> String fileToString(Class<T> beanClass, String fileName) {
        InputStream resourceAsStream = beanClass.getResourceAsStream(fileName);
        assert resourceAsStream != null;
        return CheckedFunction0.of(() -> CharStreams.toString(new InputStreamReader(resourceAsStream, Charsets.UTF_8))).unchecked().get();
    }

    protected <T> T jsonToType(String jsonString, Class<T> beanClass) {
        JsonElement jsonElement = JsonParser.parseString(jsonString);
        String key = (String) jsonElement.getAsJsonObject().keySet().toArray()[0];
        return gsonBuilder.create().fromJson(jsonElement.getAsJsonObject().get(key), beanClass);
    }

    protected <T> T jsonToType(String jsonString, Type type) {
        JsonElement jsonElement = JsonParser.parseString(jsonString);
        String key = (String) jsonElement.getAsJsonObject().keySet().toArray()[0];
        return gsonBuilder.create().fromJson(jsonElement.getAsJsonObject().get(key), type);
    }
}
