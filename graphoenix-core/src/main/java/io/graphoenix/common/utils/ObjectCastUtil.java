package io.graphoenix.common.utils;

import org.javatuples.Pair;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum ObjectCastUtil {
    OBJECT_CAST_UTIL;

    public <T> T cast(Object object, Class<T> clazz) throws ClassCastException {
        return clazz.cast(object);
    }

    public <K, V> Map<K, V> castToMap(Object object, Class<K> keyClazz, Class<V> valueClazz) throws ClassCastException {
        Map<?, ?> map = (Map<?, ?>) object;
        assert map != null;
        return map.entrySet().stream()
                .collect(
                        Collectors.toMap(
                                entry -> keyClazz.cast(entry.getKey()),
                                entry -> valueClazz.cast(entry.getValue())
                        )
                );
    }

    public <T> List<T> castToList(Object object, Class<T> clazz) throws ClassCastException {
        List<?> list = (List<?>) object;
        assert list != null;
        return list.stream().map(clazz::cast).collect(Collectors.toList());
    }

    public <T> Stream<T> castToStream(Object object, Class<T> clazz) throws ClassCastException {
        Stream<?> stream = (Stream<?>) object;
        assert stream != null;
        return stream.map(clazz::cast);
    }

    public <T> Mono<T> castToMono(Object object, Class<T> clazz) throws ClassCastException {
        Mono<?> mono = (Mono<?>) object;
        assert mono != null;
        return mono.map(clazz::cast);
    }

    public <T> Flux<T> castToFlux(Object object, Class<T> clazz) throws ClassCastException {
        Flux<?> flux = (Flux<?>) object;
        assert flux != null;
        return flux.map(clazz::cast);
    }

    public <K, V> Stream<Pair<K, V>> castToStreamPair(Object object, Class<K> clazz0, Class<V> clazz1) throws ClassCastException {
        Stream<?> stream = (Stream<?>) object;
        assert stream != null;
        return stream.filter(item -> item instanceof Pair)
                .map(item -> (Pair<?, ?>) item)
                .map(pair -> Pair.with(
                        clazz0.cast(pair.getValue0()),
                        clazz1.cast(pair.getValue1())
                        )
                );
    }

    public <K, V> Mono<Pair<K, V>> castToMonoPair(Object object, Class<K> clazz0, Class<V> clazz1) throws ClassCastException {
        Mono<?> mono = (Mono<?>) object;
        assert mono != null;
        return mono.filter(item -> item instanceof Pair)
                .map(item -> (Pair<?, ?>) item)
                .map(pair -> Pair.with(
                        clazz0.cast(pair.getValue0()),
                        clazz1.cast(pair.getValue1())
                        )
                );
    }

    public <K, V> Flux<Pair<K, V>> castToFluxPair(Object object, Class<K> clazz0, Class<V> clazz1) throws ClassCastException {
        Flux<?> flux = (Flux<?>) object;
        assert flux != null;
        return flux.filter(item -> item instanceof Pair)
                .map(item -> (Pair<?, ?>) item)
                .map(pair -> Pair.with(
                        clazz0.cast(pair.getValue0()),
                        clazz1.cast(pair.getValue1())
                        )
                );
    }
}
