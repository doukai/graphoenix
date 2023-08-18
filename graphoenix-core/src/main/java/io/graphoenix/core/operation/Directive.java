package io.graphoenix.core.operation;

import com.google.common.base.CaseFormat;
import graphql.parser.antlr.GraphqlParser;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import javax.lang.model.element.AnnotationMirror;
import java.util.Map;
import java.util.stream.Collectors;

import static io.graphoenix.core.utils.ElementUtil.ELEMENT_UTIL;

public class Directive {

    private String name;
    private Arguments arguments;

    public String getName() {
        return name;
    }

    public Directive() {
    }

    public Directive(GraphqlParser.DirectiveContext directiveContext) {
        this.name = directiveContext.name().getText();
        if (directiveContext.arguments() != null) {
            this.arguments = new Arguments(directiveContext.arguments());
        }
    }

    public Directive(AnnotationMirror annotationMirror) {
        this.name = annotationMirror.getAnnotationType().getAnnotation(io.graphoenix.spi.annotation.Directive.class).value();
        if (annotationMirror.getElementValues() != null) {
            Map<String, ValueWithVariable> arguments = annotationMirror.getElementValues().entrySet().stream().collect(Collectors.toMap(entry -> ELEMENT_UTIL.getNameFromElement(entry.getKey()), entry -> ValueWithVariable.of(entry.getValue())));
            this.arguments = new Arguments(arguments);
        }
    }

    public Directive(String name) {
        this.name = name;
    }

    public Directive setName(String name) {
        this.name = name;
        return this;
    }

    public Arguments getArguments() {
        return arguments;
    }

    public Directive setArguments(GraphqlParser.ArgumentsContext argumentsContext) {
        this.arguments = new Arguments(argumentsContext);
        return this;
    }

    public Directive setArguments(Arguments arguments) {
        this.arguments = arguments;
        return this;
    }

    public Directive setArguments(JsonObject jsonObject) {
        this.arguments = new Arguments(jsonObject);
        return this;
    }

    public Directive addArguments(Arguments arguments) {
        if (this.arguments == null) {
            this.arguments = new Arguments();
        }
        this.arguments.putAll(arguments);
        return this;
    }

    public Directive addArguments(JsonObject jsonObject) {
        if (this.arguments == null) {
            this.arguments = new Arguments();
        }
        this.arguments.putAll(jsonObject);
        return this;
    }

    public Directive addArgument(String name, Object valueWithVariable) {
        if (this.arguments == null) {
            this.arguments = new Arguments();
        }
        this.arguments.put(name, valueWithVariable);
        return this;
    }

    public Directive addArgument(String name, ValueWithVariable valueWithVariable) {
        if (this.arguments == null) {
            this.arguments = new Arguments();
        }
        this.arguments.put(name, valueWithVariable);
        return this;
    }

    public Directive addArgument(String name, JsonValue valueWithVariable) {
        if (this.arguments == null) {
            this.arguments = new Arguments();
        }
        this.arguments.put(name, valueWithVariable);
        return this;
    }

    @Override
    public String toString() {
        STGroupFile stGroupFile = new STGroupFile("stg/operation/Directive.stg");
        ST st = stGroupFile.getInstanceOf("directiveDefinition");
        st.add("directive", this);
        String render = st.render();
        stGroupFile.unload();
        return render;
    }
}
