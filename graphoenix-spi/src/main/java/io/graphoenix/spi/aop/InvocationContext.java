package io.graphoenix.spi.aop;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class InvocationContext {

    private String name;

    private Object target;

    private Map<String, Object> parameterValues;

    private Object returnValue;

    private Class<?> owner;

    private Map<String, Object> ownerValues;

    private Map<String, Object> extensions;

    public String getName() {
        return name;
    }

    public InvocationContext setName(String name) {
        this.name = name;
        return this;
    }

    public Object getTarget() {
        return target;
    }

    public InvocationContext setTarget(Object target) {
        this.target = target;
        return this;
    }

    public Map<String, Object> getParameterValueMap() {
        return parameterValues;
    }

    public Set<String> getParameterNames() {
        return parameterValues.keySet();
    }

    public Collection<Object> getParameterValues() {
        return parameterValues.values();
    }

    public Object getParameterValue(String parameterName) {
        return parameterValues.get(parameterName);
    }

    public InvocationContext addParameterValue(String parameterName, Object parameterValue) {
        if (this.parameterValues == null) {
            this.parameterValues = new HashMap<>();
        }
        parameterValues.put(parameterName, parameterValue);
        return this;
    }

    public InvocationContext setParameterValues(Map<String, Object> parameterValues) {
        this.parameterValues = parameterValues;
        return this;
    }

    public Object getReturnValue() {
        return returnValue;
    }

    public InvocationContext setReturnValue(Object returnValue) {
        this.returnValue = returnValue;
        return this;
    }

    public Class<?> getOwner() {
        return owner;
    }

    public InvocationContext setOwner(Class<?> owner) {
        this.owner = owner;
        return this;
    }

    public Map<String, Object> getOwnerValues() {
        return ownerValues;
    }

    public Object getOwnerValue(String name) {
        return ownerValues.get(name);
    }

    public InvocationContext setOwnerValues(Map<String, Object> ownerValues) {
        this.ownerValues = ownerValues;
        return this;
    }

    public InvocationContext addOwnerValue(String name, Object value) {
        if (this.ownerValues == null) {
            this.ownerValues = new HashMap<>();
        }
        ownerValues.put(name, value);
        return this;
    }

    public Map<String, Object> getExtensions() {
        return extensions;
    }

    public InvocationContext setExtensions(Map<String, Object> extensions) {
        this.extensions = extensions;
        return this;
    }
}
