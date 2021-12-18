package io.graphoenix.common.utils;

import java.io.*;
import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaConversionException;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class GPXBeanContext {

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    private static final Map<Class<?>, Supplier<?>> CACHE = new ConcurrentHashMap<>();
    private static final Map<String, String> FACTORIES = new HashMap<>();

    static {
        InputStream resourceAsStream = GPXBeanContext.class.getResourceAsStream("/META-INF/graphoenix.factories");
        assert resourceAsStream != null;
        BufferedReader reader = new BufferedReader(new InputStreamReader(resourceAsStream));
        try {
            while (reader.ready()) {
                String[] factoryPair = reader.readLine().split("=");
                FACTORIES.putIfAbsent(factoryPair[0], factoryPair[1]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private GPXBeanContext() {
    }

    public static <T> T get(Class<T> beanClass) {
        return beanClass.cast(getCachedSupplier(beanClass).get());
    }

    private static <T> Supplier<?> getCachedSupplier(Class<T> beanClass) {
        final Supplier<?> supplier = CACHE.get(beanClass);
        if (supplier != null) {
            return supplier;
        }
        return createAndCacheSupplier(beanClass);
    }

    private static <T> Supplier<?> createAndCacheSupplier(Class<T> beanClass) {
        return cacheAndGetSupplier(beanClass, createSupplier(beanClass));
    }

    private static <T> Supplier<?> cacheAndGetSupplier(Class<T> beanClass, Supplier<?> supplierToBeCached) {
        Supplier<?> cachedSupplier = CACHE.putIfAbsent(beanClass, supplierToBeCached);
        return cachedSupplier != null ? cachedSupplier : supplierToBeCached;
    }

    private static <T> Supplier<?> createSupplier(Class<T> beanClass) {
        return Stream.of(getFactoryClass(beanClass).orElseThrow().getDeclaredMethods())
                .filter(method -> isCreateMethod(beanClass, method))
                .map(GPXBeanContext::creatorMethodToSupplier)
                .findFirst()
                .orElseThrow(IllegalStateException::new);
    }

    private static <T> Optional<Class<?>> getFactoryClass(Class<T> beanClass) {
        try {
            return Optional.of(Class.forName(FACTORIES.get(beanClass.getName())));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    private static <T> boolean isCreateMethod(Class<T> beanClass, Method method) {
        return method.getParameterCount() == 0 &&
                Modifier.isStatic(method.getModifiers()) &&
                method.getReturnType().isInstance(beanClass);
    }

    private static Supplier<?> creatorMethodToSupplier(Method creatorMethod) {
        try {
            return (Supplier<?>) createCallSite(LOOKUP.unreflect(creatorMethod)).getTarget().invokeExact();
        } catch (Throwable e) {
            throw new IllegalArgumentException("Lambda creation failed for creatorMethod (" + creatorMethod.getName() + ").", e);
        }
    }

    private static CallSite createCallSite(MethodHandle createSupplierHandle) throws LambdaConversionException {
        return LambdaMetafactory.metafactory(
                LOOKUP,
                "get",
                MethodType.methodType(Supplier.class),
                MethodType.methodType(Object.class),
                createSupplierHandle,
                createSupplierHandle.type()
        );
    }
}
