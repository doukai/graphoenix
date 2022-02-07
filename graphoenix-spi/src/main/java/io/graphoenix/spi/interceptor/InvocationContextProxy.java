package io.graphoenix.spi.interceptor;

import io.vavr.CheckedConsumer;
import io.vavr.CheckedFunction1;
import jakarta.interceptor.InvocationContext;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public class InvocationContextProxy implements InvocationContext {

    private Class<?> owner;

    private Map<String, Object> ownerValues;

    private Object target;

    private Object timer;

    private Method method;

    private Constructor<?> constructor;

    private Map<String, Object> parameterMap;

    private Map<String, Object> contextData;

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
        return ownerValues;
    }

    public Object getOwnerValue(String name) {
        return ownerValues.get(name);
    }

    public InvocationContextProxy setOwnerValues(Map<String, Object> ownerValues) {
        this.ownerValues = ownerValues;
        return this;
    }

    public InvocationContextProxy addOwnerValue(String name, Object value) {
        if (this.ownerValues == null) {
            this.ownerValues = new HashMap<>();
        }
        ownerValues.put(name, value);
        return this;
    }

    public InvocationContextProxy setTarget(Object target) {
        this.target = target;
        return this;
    }

    public InvocationContextProxy setTimer(Object timer) {
        this.timer = timer;
        return this;
    }

    public InvocationContextProxy setMethod(String methodName, int parameterCount, List<String> parameterNameList, List<String> parameterTypeNameList) {
        this.method = Arrays.stream(getTarget().getClass().getMethods())
                .filter(method -> method.getName().equals(methodName))
                .filter(method -> method.getParameterCount() == parameterCount)
                .filter(method ->
                        IntStream.range(0, method.getParameterCount() - 1)
                                .allMatch(index ->
                                        method.getParameters()[index].getName().equals(parameterNameList.get(index)) &&
                                                method.getParameters()[index].getType().getName().equals(parameterTypeNameList.get(index))
                                )
                )
                .findFirst()
                .orElseThrow();

        return this;
    }

    public InvocationContextProxy setConstructor(int parameterCount, List<String> parameterNameList, List<String> parameterTypeNameList) {
        this.constructor = Arrays.stream(getTarget().getClass().getConstructors())
                .filter(constructor -> constructor.getParameterCount() == parameterCount)
                .filter(constructor ->
                        IntStream.range(0, constructor.getParameterCount() - 1)
                                .allMatch(index ->
                                        constructor.getParameters()[index].getName().equals(parameterNameList.get(index)) &&
                                                constructor.getParameters()[index].getType().getName().equals(parameterTypeNameList.get(index))
                                )
                )
                .findFirst()
                .orElseThrow();

        return this;
    }

    public InvocationContextProxy setContextData(Map<String, Object> contextData) {
        this.contextData = contextData;
        return this;
    }

    public Object getParameterValue(String parameterName) {
        return parameterMap.get(parameterName);
    }

    public InvocationContextProxy addParameterValue(String parameterName, Object parameterValue) {
        if (parameterMap == null) {
            parameterMap = new HashMap<>();
        }
        parameterMap.put(parameterName, parameterValue);
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
        return target;
    }

    @Override
    public Object getTimer() {
        return timer;
    }

    @Override
    public Method getMethod() {
        return method;
    }

    @Override
    public Constructor<?> getConstructor() {
        return constructor;
    }

    @Override
    public Object[] getParameters() {
        return parameterMap.values().toArray();
    }

    @Override
    public void setParameters(Object[] params) {
        if (params != null) {
            if (parameterMap == null) {
                parameterMap = new HashMap<>();
            }
            IntStream.range(0, method.getParameterCount() - 1).forEach(index -> parameterMap.put(method.getParameters()[index].getName(), params[index]));
        }
    }

    @Override
    public Map<String, Object> getContextData() {
        return contextData;
    }

    @Override
    public Object proceed() throws Exception {
        try {
            if (function != null) {
                return function.apply(this);
            } else if (consumer != null) {
                consumer.accept(this);
                return null;
            } else {
                return nextProceed.apply(nextInvocationContext);
            }
        } catch (Throwable throwable) {
            throw new Exception(throwable);
        }
    }
}
