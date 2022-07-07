package io.graphoenix.core.document;

import graphql.parser.antlr.GraphqlParser;
import org.antlr.v4.runtime.RuleContext;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.graphoenix.core.utils.DocumentUtil.DOCUMENT_UTIL;

public class InputObjectType {

    private String name;
    private Set<String> directives;
    private Set<InputValue> inputValues;
    private String description;

    public InputObjectType() {
    }

    public InputObjectType(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext) {
        this.name = inputObjectTypeDefinitionContext.name().getText();
        if (inputObjectTypeDefinitionContext.description() != null) {
            this.description = DOCUMENT_UTIL.getStringValue(inputObjectTypeDefinitionContext.description().StringValue());
        }
        if (inputObjectTypeDefinitionContext.directives() != null) {
            this.directives = inputObjectTypeDefinitionContext.directives().directive().stream().map(RuleContext::getText).collect(Collectors.toSet());
        }
        if (inputObjectTypeDefinitionContext.inputObjectValueDefinitions() != null) {
            this.inputValues = inputObjectTypeDefinitionContext.inputObjectValueDefinitions().inputValueDefinition().stream().map(InputValue::new).collect(Collectors.toSet());
        }
    }

    public static InputObjectType merge(GraphqlParser.InputObjectTypeDefinitionContext... inputObjectTypeDefinitionContexts) {
        return merge(Stream.of(inputObjectTypeDefinitionContexts).map(InputObjectType::new).toArray(InputObjectType[]::new));
    }

    public static InputObjectType merge(InputObjectType... inputObjectTypes) {
        InputObjectType inputObjectType = new InputObjectType();
        inputObjectType.name = inputObjectTypes[0].getName();
        inputObjectType.description = inputObjectTypes[0].getDescription();
        inputObjectType.directives = Stream.of(inputObjectTypes).flatMap(item -> item.getDirectives().stream()).collect(Collectors.toSet());
        inputObjectType.inputValues = Stream.of(inputObjectTypes).flatMap(item -> item.getInputValues().stream()).collect(Collectors.toSet());
        return inputObjectType;
    }

    public String getName() {
        return name;
    }

    public InputObjectType setName(String name) {
        this.name = name;
        return this;
    }

    public Set<String> getDirectives() {
        return directives;
    }

    public InputObjectType setDirectives(Set<String> directives) {
        this.directives = directives;
        return this;
    }

    public Set<InputValue> getInputValues() {
        return inputValues;
    }

    public InputObjectType setInputValues(Set<InputValue> inputValues) {
        this.inputValues = inputValues;
        return this;
    }

    public InputObjectType addInputValue(InputValue inputValue) {
        if (this.inputValues == null) {
            this.inputValues = new LinkedHashSet<>();
        }
        this.inputValues.add(inputValue);
        return this;
    }

    public InputObjectType addInputValues(Set<InputValue> inputValues) {
        if (this.inputValues == null) {
            this.inputValues = new LinkedHashSet<>();
        }
        this.inputValues.addAll(inputValues);
        return this;
    }

    public String getDescription() {
        return description;
    }

    public InputObjectType setDescription(String description) {
        this.description = description;
        return this;
    }

    @Override
    public String toString() {
        STGroupFile stGroupFile = new STGroupFile("stg/document/InputObjectType.stg");
        ST st = stGroupFile.getInstanceOf("inputObjectTypeDefinition");
        st.add("inputObjectType", this);
        String render = st.render();
        stGroupFile.unload();
        return render;
    }
}
