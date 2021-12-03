package io.graphoenix.spi.handler;

import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import org.javatuples.Pair;
import org.reactivestreams.Publisher;

import java.util.Map;
import java.util.stream.Stream;

public interface IPipelineContext {

    IPipelineContext getInstance();

    IGraphQLDocumentManager getManager();

    void setManager(IGraphQLDocumentManager manager);

    void addStatus(Enum<?> status);

    <T> T getStatus(Class<T> clazz) throws ClassCastException;

    IPipelineContext add(Object object);

    <T> T poll(Class<T> clazz);

    <K, V> Map<K, V> pollMap(Class<K> keyClazz, Class<V> valueClazz) throws ClassCastException;

    <T> Stream<T> pollStream(Class<T> clazz) throws ClassCastException;

    <T> Publisher<T> pollMono(Class<T> clazz) throws ClassCastException;

    <T> Publisher<T> pollFlux(Class<T> clazz) throws ClassCastException;

    <K, V> Stream<Pair<K, V>> pollStreamPair(Class<K> clazz0, Class<V> clazz1) throws ClassCastException;

    <K, V> Publisher<Pair<K, V>> pollMonoPair(Class<K> clazz0, Class<V> clazz1) throws ClassCastException;

    <K, V> Publisher<Pair<K, V>> pollFluxPair(Class<K> clazz0, Class<V> clazz1) throws ClassCastException;

    Object poll();

    <T> T element(Class<T> clazz) throws ClassCastException;

    Object element();
}
