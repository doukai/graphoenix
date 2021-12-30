package io.graphoenix.core.factory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

public class Operations {

    private static final String IMPLEMENTATION_SUFFIX = "Impl";

    private Operations() {
    }

    /**
     * Returns an instance of the given operation type.
     *
     * @param clazz The type of the operation to return.
     * @param <T> The type of the operation to create.
     *
     * @return An instance of the given operation type.
     */
    public static <T> T getOperation(Class<T> clazz) {
        try {
            List<ClassLoader> classLoaders = collectClassLoaders( clazz.getClassLoader() );

            return getOperation( clazz, classLoaders );
        }
        catch ( ClassNotFoundException | NoSuchMethodException e ) {
            throw new RuntimeException( e );
        }
    }

    private static <T> T getOperation(Class<T> operationType, Iterable<ClassLoader> classLoaders)
            throws ClassNotFoundException, NoSuchMethodException {

        for ( ClassLoader classLoader : classLoaders ) {
            T operation = doGetOperation( operationType, classLoader );
            if ( operation != null ) {
                return operation;
            }
        }

        throw new ClassNotFoundException("Cannot find implementation for " + operationType.getName() );
    }

    private static <T> T doGetOperation(Class<T> clazz, ClassLoader classLoader) throws NoSuchMethodException {
        try {
            @SuppressWarnings( "unchecked" )
            Class<T> implementation = (Class<T>) classLoader.loadClass( clazz.getName() + IMPLEMENTATION_SUFFIX );
            Constructor<T> constructor = implementation.getDeclaredConstructor();
            constructor.setAccessible( true );

            return constructor.newInstance();
        }
        catch (ClassNotFoundException e) {
            return getOperationFromServiceLoader( clazz, classLoader );
        }
        catch ( InstantiationException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException( e );
        }
    }

    /**
     * Returns the class of the implementation for the given operation type.
     *
     * @param clazz The type of the operation to return.
     * @param <T> The type of the operation to create.
     *
     * @return A class of the implementation for the given operation type.
     *
     * @since 1.3
     */
    public static <T> Class<? extends T> getOperationClass(Class<T> clazz) {
        try {
            List<ClassLoader> classLoaders = collectClassLoaders( clazz.getClassLoader() );

            return getOperationClass( clazz, classLoaders );
        }
        catch ( ClassNotFoundException e ) {
            throw new RuntimeException( e );
        }
    }

    private static <T> Class<? extends T> getOperationClass(Class<T> operationType, Iterable<ClassLoader> classLoaders)
            throws ClassNotFoundException {

        for ( ClassLoader classLoader : classLoaders ) {
            Class<? extends T> operationClass = doGetOperationClass( operationType, classLoader );
            if ( operationClass != null ) {
                return operationClass;
            }
        }

        throw new ClassNotFoundException( "Cannot find implementation for " + operationType.getName() );
    }

    @SuppressWarnings("unchecked")
    private static <T> Class<? extends T> doGetOperationClass(Class<T> clazz, ClassLoader classLoader) {
        try {
            return (Class<? extends T>) classLoader.loadClass( clazz.getName() + IMPLEMENTATION_SUFFIX );
        }
        catch ( ClassNotFoundException e ) {
            T operation = getOperationFromServiceLoader( clazz, classLoader );
            if ( operation != null ) {
                return (Class<? extends T>) operation.getClass();
            }

            return null;
        }
    }

    private static <T> T getOperationFromServiceLoader(Class<T> clazz, ClassLoader classLoader) {
        ServiceLoader<T> loader = ServiceLoader.load( clazz, classLoader );

        for ( T operation : loader ) {
            if ( operation != null ) {
                return operation;
            }
        }

        return null;
    }

    private static List<ClassLoader> collectClassLoaders(ClassLoader classLoader) {
        List<ClassLoader> classLoaders = new ArrayList<>( 3 );
        classLoaders.add( classLoader );

        if ( Thread.currentThread().getContextClassLoader() != null ) {
            classLoaders.add( Thread.currentThread().getContextClassLoader() );
        }

        classLoaders.add( Operations.class.getClassLoader() );

        return classLoaders;
    }
}
