package io.graphoenix.core.context;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.util.TypeLiteral;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.util.Iterator;

public abstract class RequestInstance<T> implements Instance<T> {
    @Override
    public Instance<T> select(Annotation... qualifiers) {
        return null;
    }

    @Override
    public <U extends T> Instance<U> select(Class<U> subtype, Annotation... qualifiers) {
        return null;
    }

    @Override
    public <U extends T> Instance<U> select(TypeLiteral<U> subtype, Annotation... qualifiers) {
        return null;
    }

    @Override
    public boolean isUnsatisfied() {
        return false;
    }

    @Override
    public boolean isAmbiguous() {
        return false;
    }

    @Override
    public void destroy(T instance) {

    }

    @Override
    public T get() {
        return stream().findFirst().orElse(null);
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
        return stream().iterator();
    }
}
