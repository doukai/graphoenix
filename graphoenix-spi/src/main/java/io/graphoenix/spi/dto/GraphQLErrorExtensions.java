package io.graphoenix.spi.dto;

import java.time.LocalDateTime;

public class GraphQLErrorExtensions {

    private Integer code;

    private LocalDateTime timestamp;

    public GraphQLErrorExtensions() {
    }

    public GraphQLErrorExtensions(Integer code) {
        this.code = code;
        this.timestamp = LocalDateTime.now();
    }

    public GraphQLErrorExtensions(Integer code, LocalDateTime timestamp) {
        this.code = code;
        this.timestamp = timestamp;
    }

    public Integer getCode() {
        return code;
    }

    public GraphQLErrorExtensions setCode(Integer code) {
        this.code = code;
        return this;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public GraphQLErrorExtensions setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
        return this;
    }
}
