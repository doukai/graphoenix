package io.graphoenix.spi.dto;

public class SelectionResult<T> {

    private String name;
    private T data;

    public SelectionResult(String name, T data) {
        this.name = name;
        this.data = data;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
