package io.graphoenix.core.dto;

import com.dslplatform.json.CompiledJson;
import com.dslplatform.json.JsonAttribute;
import com.dslplatform.json.runtime.ObjectAnalyzer;
import io.graphoenix.spi.dto.GraphQLError;

import java.util.List;

@CompiledJson
public class GraphQLResponse {

    @JsonAttribute(converter = ObjectAnalyzer.Runtime.class)
    private Object data;

    public List<GraphQLError> errors;

    public GraphQLResponse() {
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public List<GraphQLError> getErrors() {
        return errors;
    }

    public void setErrors(List<GraphQLError> errors) {
        this.errors = errors;
    }
}
