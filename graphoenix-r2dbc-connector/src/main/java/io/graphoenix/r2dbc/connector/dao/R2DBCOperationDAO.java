package io.graphoenix.r2dbc.connector.dao;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.r2dbc.connector.executor.MutationExecutor;
import io.graphoenix.r2dbc.connector.executor.QueryExecutor;
import io.graphoenix.r2dbc.connector.parameter.R2dbcParameterProcessor;
import io.graphoenix.spi.dao.BaseOperationDAO;
import reactor.core.publisher.Mono;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

public class R2DBCOperationDAO extends BaseOperationDAO {

    private final GsonBuilder gsonBuilder;

    private final QueryExecutor queryExecutor;

    private final MutationExecutor mutationExecutor;

    private final R2dbcParameterProcessor r2dbcParameterProcessor;

    public R2DBCOperationDAO() {
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
        this.queryExecutor = BeanContext.get(QueryExecutor.class);
        this.mutationExecutor = BeanContext.get(MutationExecutor.class);
        this.r2dbcParameterProcessor = BeanContext.get(R2dbcParameterProcessor.class);
    }

    @Override
    public <T> T findOne(String sql, Map<String, Object> parameters, Class<T> beanClass) {
        JsonElement jsonElement = JsonParser.parseString(Objects.requireNonNull(queryExecutor.executeQuery(sql, r2dbcParameterProcessor.process(parameters)).block()));
        String key = (String) jsonElement.getAsJsonObject().keySet().toArray()[0];
        return gsonBuilder.create().fromJson(jsonElement.getAsJsonObject().get(key), beanClass);
    }

    @Override
    public <T> T findAll(String sql, Map<String, Object> parameters, Type type) {
        JsonElement jsonElement = JsonParser.parseString(Objects.requireNonNull(queryExecutor.executeQuery(sql, r2dbcParameterProcessor.process(parameters)).block()));
        String key = (String) jsonElement.getAsJsonObject().keySet().toArray()[0];
        return gsonBuilder.create().fromJson(jsonElement.getAsJsonObject().get(key), type);
    }

    @Override
    public <T> Collection<T> findAll(String sql, Map<String, Object> parameters, Class<T> beanClass) {
        Type type = (new TypeToken<Collection<T>>() {
        }).getType();
        return findAll(sql, parameters, type);
    }

    @Override
    public <T> T save(String sql, Map<String, Object> parameters, Class<T> beanClass) {
        JsonElement jsonElement = JsonParser.parseString(Objects.requireNonNull(mutationExecutor.executeMutations(sql, r2dbcParameterProcessor.process(parameters)).block()));
        String key = (String) jsonElement.getAsJsonObject().keySet().toArray()[0];
        return gsonBuilder.create().fromJson(jsonElement.getAsJsonObject().get(key), beanClass);
    }

    @Override
    public <T> Mono<T> findOneAsync(String sql, Map<String, Object> parameters, Class<T> beanClass) {
        return queryExecutor.executeQuery(sql, r2dbcParameterProcessor.process(parameters))
                .map(json -> {
                            JsonElement jsonElement = JsonParser.parseString(json);
                            String key = (String) jsonElement.getAsJsonObject().keySet().toArray()[0];
                            return gsonBuilder.create().fromJson(jsonElement.getAsJsonObject().get(key), beanClass);
                        }
                );
    }

    @Override
    public <T> Mono<T> findAllAsync(String sql, Map<String, Object> parameters, Type type) {
        Mono<String> jsonMono = queryExecutor.executeQuery(sql, r2dbcParameterProcessor.process(parameters));
        return jsonMono.map(json -> {
                    JsonElement jsonElement = JsonParser.parseString(json);
                    String key = (String) jsonElement.getAsJsonObject().keySet().toArray()[0];
                    return gsonBuilder.create().fromJson(jsonElement.getAsJsonObject().get(key), type);
                }
        );
    }

    @Override
    public <T> Mono<Collection<T>> findAllAsync(String sql, Map<String, Object> parameters, Class<T> beanClass) {
        Type type = (new TypeToken<Collection<T>>() {
        }).getType();
        return findAllAsync(sql, parameters, type);
    }

    @Override
    public <T> Mono<T> saveAsync(String sql, Map<String, Object> parameters, Class<T> beanClass) {
        Mono<String> jsonMono = mutationExecutor.executeMutations(sql, r2dbcParameterProcessor.process(parameters));
        return jsonMono.map(json -> {
                    JsonElement jsonElement = JsonParser.parseString(json);
                    String key = (String) jsonElement.getAsJsonObject().keySet().toArray()[0];
                    return gsonBuilder.create().fromJson(jsonElement.getAsJsonObject().get(key), beanClass);
                }
        );
    }
}
