package io.graphoenix.spi.patterns;

public interface CompositeBeanBuilder {

    String MESSAGE = "only use for build bean";

    static CompositeBeanBuilder create() {
        throw new RuntimeException(MESSAGE);
    }

    void put(String methodName, Object bean);

    void put(String methodName, Object bean, String beanMethodName);

    <T> T build(Class<T> beanClass);
}
