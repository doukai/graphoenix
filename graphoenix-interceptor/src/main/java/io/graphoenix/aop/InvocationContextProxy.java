package io.graphoenix.aop;

import io.vavr.CheckedConsumer;
import io.vavr.CheckedFunction1;
import jakarta.interceptor.InvocationContext;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

public class InvocationContextProxy implements InvocationContext {

    private Class<?> owner;

    private Map<String, Object> ownerValues = new HashMap<>();

    private Object target;

    private Class<?> targetClass;

    private Object timer;

    private Method method;

    private Constructor<?> constructor;

    private Map<String, Object> parameterMap = new HashMap<>();

    private Map<String, Object> contextData = new HashMap<>();

    private CheckedFunction1<InvocationContext, Object> function;

    private CheckedConsumer<InvocationContext> consumer;

    private CheckedFunction1<InvocationContext, Object> nextProceed;

    private InvocationContext nextInvocationContext;

    public Class<?> getOwner() {
        return owner;
    }

    public InvocationContextProxy setOwner(Class<?> owner) {
        this.owner = owner;
        return this;
    }

    public Map<String, Object> getOwnerValues() {
        return this.ownerValues;
    }

    public Object getOwnerValue(String name) {
        return this.ownerValues.get(name);
    }

    public InvocationContextProxy setOwnerValues(Map<String, Object> ownerValues) {
        this.ownerValues = ownerValues;
        this.contextData.putAll(ownerValues);
        return this;
    }

    public InvocationContextProxy addOwnerValue(String name, Object value) {
        this.ownerValues.put(name, value);
        this.contextData.put(name, value);
        return this;
    }

    public InvocationContextProxy setTarget(Object target) {
        this.target = target;
        if (target instanceof Class<?>) {
            this.targetClass = (Class<?>) target;
        } else {
            this.targetClass = target.getClass();
        }
        return this;
    }

    public InvocationContextProxy setTimer(Object timer) {
        this.timer = timer;
        return this;
    }

    public InvocationContextProxy setMethod(Method method) {
        this.method = method;
        return this;
    }

    public InvocationContextProxy setMethod(String methodName, int parameterCount, String[] parameterTypeNames) {
        this.method = Arrays.stream(this.targetClass.getMethods())
                .filter(method -> method.getName().equals(methodName))
                .filter(method -> method.getParameterCount() == parameterCount)
                .filter(method ->
                        IntStream.range(0, method.getParameterCount() - 1)
                                .allMatch(index -> this.method.getParameters()[index].getType().getName().equals(parameterTypeNames[index]))
                )
                .findFirst()
                .orElseThrow(NoSuchMethodError::new);
        return this;
    }

    public InvocationContextProxy setConstructor(Constructor<?> constructor) {
        this.constructor = constructor;
        return this;
    }

    public InvocationContextProxy setConstructor(int parameterCount, String[] parameterTypeNames) {
        this.constructor = Arrays.stream(this.targetClass.getConstructors())
                .filter(constructor -> constructor.getParameterCount() == parameterCount)
                .filter(constructor ->
                        IntStream.range(0, constructor.getParameterCount() - 1)
                                .allMatch(index -> this.constructor.getParameters()[index].getType().getName().equals(parameterTypeNames[index]))
                )
                .findFirst()
                .orElseThrow(NoSuchMethodError::new);

        return this;
    }

    public InvocationContextProxy setContextData(Map<String, Object> contextData) {
        this.contextData = contextData;
        return this;
    }

    public Object getParameterValue(String parameterName) {
        return this.parameterMap.get(parameterName);
    }

    public InvocationContextProxy setParameterMap(Map<String, Object> parameterMap) {
        this.parameterMap = parameterMap;
        return this;
    }

    public InvocationContextProxy addParameterValue(String parameterName, Object parameterValue) {
        this.parameterMap.put(parameterName, parameterValue);
        return this;
    }

    public InvocationContextProxy setFunction(CheckedFunction1<InvocationContext, Object> function) {
        this.function = function;
        return this;
    }

    public InvocationContextProxy setConsumer(CheckedConsumer<InvocationContext> consumer) {
        this.consumer = consumer;
        return this;
    }

    public InvocationContextProxy setNextProceed(CheckedFunction1<InvocationContext, Object> nextProceed) {
        this.nextProceed = nextProceed;
        return this;
    }

    public InvocationContextProxy setNextInvocationContext(InvocationContext nextInvocationContext) {
        this.nextInvocationContext = nextInvocationContext;
        return this;
    }

    @Override
    public Object getTarget() {
        return this.target;
    }

    @Override
    public Object getTimer() {
        return this.timer;
    }

    @Override
    public Method getMethod() {
        return this.method;
    }

    @Override
    public Constructor<?> getConstructor() {
        return this.constructor;
    }

    @Override
    public Object[] getParameters() {
        return this.parameterMap.values().toArray();
    }

    @Override
    public void setParameters(Object[] params) {
        if (params != null) {
            IntStream.range(0, this.method.getParameterCount() - 1).forEach(index -> this.parameterMap.put(this.method.getParameters()[index].getName(), params[index]));
        }
    }

    @Override
    public Map<String, Object> getContextData() {
        return this.contextData;
    }

    @Override
    public Object proceed() throws Exception {
        try {
            if (this.function != null) {
                return this.function.apply(this);
            } else if (this.consumer != null) {
                this.consumer.accept(this);
                return null;
            } else {
                ((InvocationContextProxy) this.nextInvocationContext).setParameterMap(this.parameterMap).setContextData(this.contextData).setMethod(this.method).setConstructor(this.constructor);
                return this.nextProceed.apply(this.nextInvocationContext);
            }
        } catch (Throwable throwable) {
            throw new Exception(throwable);
        }
    }
}
