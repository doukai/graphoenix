package io.graphoenix.core.utils;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum ObjectCastUtil {
    OBJECT_CAST_UTIL;

    public <T> T cast(Object object, Class<T> objectClass) throws ClassCastException {
        return objectClass.cast(object);
    }

    public <K, V> Map<K, V> castToMap(Object object, Class<K> keyClass, Class<V> valueClass) throws ClassCastException {
        Map<?, ?> map = (Map<?, ?>) object;
        assert map != null;
        return map.entrySet().stream()
                .collect(
                        Collectors.toMap(
                                entry -> keyClass.cast(entry.getKey()),
                                entry -> valueClass.cast(entry.getValue())
                        )
                );
    }

    public <T> List<T> castToList(Object object, Class<T> objectClass) throws ClassCastException {
        List<?> list = (List<?>) object;
        assert list != null;
        return list.stream().map(objectClass::cast).collect(Collectors.toList());
    }

    public <T> Set<T> castToSet(Object object, Class<T> objectClass) throws ClassCastException {
        Set<?> set = (Set<?>) object;
        assert set != null;
        return set.stream().map(objectClass::cast).collect(Collectors.toSet());
    }

    public <T, C extends Collection<T>> Collection<T> castToCollection(Object object, Class<T> objectClass, Supplier<C> collectionFactory) throws ClassCastException {
        Collection<?> collection = (Collection<?>) object;
        assert collection != null;
        return collection.stream().map(objectClass::cast).collect(Collectors.toCollection(collectionFactory));
    }

    public <T> Stream<T> castToStream(Object object, Class<T> objectClass) throws ClassCastException {
        Stream<?> stream = (Stream<?>) object;
        assert stream != null;
        return stream.map(objectClass::cast);
    }

    public <T> Mono<T> castToMono(Object object, Class<T> objectClass) throws ClassCastException {
        Mono<?> mono = (Mono<?>) object;
        assert mono != null;
        return mono.map(objectClass::cast);
    }

    public <T> Flux<T> castToFlux(Object object, Class<T> objectClass) throws ClassCastException {
        Flux<?> flux = (Flux<?>) object;
        assert flux != null;
        return flux.map(objectClass::cast);
    }
}
