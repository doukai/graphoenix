package io.graphoenix.common.utils;

import org.javatuples.Pair;

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaConversionException;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class GPXBeanContext {

    private static final Pattern FIELD_SEPARATOR = Pattern.compile("\\.");
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    private static final ClassValue<Function<?, ?>> CACHE = new ClassValue<>() {
        @Override
        protected Function<?, ?> computeValue(Class<?> type) {
            return Function.identity();
        }
    };

    private GPXBeanContext() {
    }


    public static <T> T getBean(Class<T> beanClass) {
        return null;
    }

    private static Function<?, ?> getCachedFunction(Class<?> javaBeanClass) {
        final Function<?, ?> function = CACHE.get(javaBeanClass);
        if (function != null) {
            return function;
        }
        return createAndCacheFunction(javaBeanClass);
    }

    private static Function<?, ?> createAndCacheFunction(Class<?> javaBeanClass) {
        return cacheAndGetFunction(javaBeanClass,
                createFunctions(javaBeanClass)
                        .stream()
                        .reduce(Function::andThen)
                        .orElseThrow(IllegalStateException::new)
        );
    }

    private static Function<?, ?> cacheAndGetFunction(String path, Class<?> javaBeanClass, Function<?, ?> functionToBeCached) {
        Function<?, ?> cachedFunction = CACHE.get(javaBeanClass).putIfAbsent(path, functionToBeCached);
        return cachedFunction != null ? cachedFunction : functionToBeCached;
    }

    private static List<Function<?, ?>> createFunctions(Class<?> javaBeanClass, String path) {
        List<Function<?, ?>> functions = new ArrayList<>();
        Stream.of(FIELD_SEPARATOR.split(path))
                .reduce(javaBeanClass, (nestedJavaBeanClass, fieldName) -> {
                    Pair<? extends Class<?>, Function<?, ?>> getFunction = createFunction(nestedJavaBeanClass);
                    functions.add(getFunction.getValue1());
                    return getFunction.getValue0();
                }, (previousClass, nextClass) -> nextClass);
        return functions;
    }

    private static Pair<? extends Class<?>, Function<?, ?>> createFunction(Class<?> clazz) {
        return Stream.of(clazz.getDeclaredMethods())
                .filter(method -> isCreateMethod(clazz, method))
                .map(GPXBeanContext::createTupleWithReturnTypeAndGetter)
                .findFirst()
                .orElseThrow(IllegalStateException::new);
    }

    private static <T> boolean isCreateMethod(Class<T> clazz, Method method) {
        return method.getParameterCount() == 0 &&
                Modifier.isStatic(method.getModifiers()) &&
                method.getReturnType().isInstance(clazz);
    }

    private static Pair<? extends Class<?>, Function<?, ?>> createTupleWithReturnTypeAndGetter(Method getterMethod) {
        try {
            return Pair.with(
                    getterMethod.getReturnType(),
                    (Function<?, ?>) createCallSite(LOOKUP.unreflect(getterMethod)).getTarget().invokeExact()
            );
        } catch (Throwable e) {
            throw new IllegalArgumentException("Lambda creation failed for getterMethod (" + getterMethod.getName() + ").", e);
        }
    }

    private static CallSite createCallSite(MethodHandle getterMethodHandle) throws LambdaConversionException {
        return LambdaMetafactory.metafactory(LOOKUP, "apply",
                MethodType.methodType(Function.class),
                MethodType.methodType(Object.class, Object.class),
                getterMethodHandle, getterMethodHandle.type());
    }
}
