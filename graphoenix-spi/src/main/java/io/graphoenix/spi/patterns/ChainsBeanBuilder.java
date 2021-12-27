package io.graphoenix.spi.patterns;

public interface ChainsBeanBuilder {

    String MESSAGE = "only use for build bean";

    static ChainsBeanBuilder create() {
        throw new RuntimeException(MESSAGE);
    }

    void add(String methodName, Object bean);

    void add(String methodName, Object bean, String beanMethodName);

    <T> T build(Class<T> beanClass);
}
