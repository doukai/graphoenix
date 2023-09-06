package io.graphoenix.core.document;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.operation.Directive;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.graphoenix.core.utils.DocumentUtil.DOCUMENT_UTIL;

public class InputObjectType {

    private String name;
    private Collection<Directive> directives;
    private Collection<InputValue> inputValues;
    private String description;

    public InputObjectType() {
    }

    public InputObjectType(String name) {
        this.name = name;
    }

    public InputObjectType(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext) {
        this.name = inputObjectTypeDefinitionContext.name().getText();
        if (inputObjectTypeDefinitionContext.description() != null) {
            this.description = DOCUMENT_UTIL.getStringValue(inputObjectTypeDefinitionContext.description().StringValue());
        }
        if (inputObjectTypeDefinitionContext.directives() != null) {
            this.directives = inputObjectTypeDefinitionContext.directives().directive().stream().map(Directive::new).collect(Collectors.toCollection(LinkedHashSet::new));
        }
        if (inputObjectTypeDefinitionContext.inputObjectValueDefinitions() != null) {
            this.inputValues = inputObjectTypeDefinitionContext.inputObjectValueDefinitions().inputValueDefinition().stream().map(InputValue::new).collect(Collectors.toCollection(LinkedHashSet::new));
        }
    }

    public static InputObjectType merge(GraphqlParser.InputObjectTypeDefinitionContext... inputObjectTypeDefinitionContexts) {
        return merge(Stream.of(inputObjectTypeDefinitionContexts).map(InputObjectType::new).toArray(InputObjectType[]::new));
    }

    public static InputObjectType merge(InputObjectType... inputObjectTypes) {
        InputObjectType inputObjectType = new InputObjectType();
        inputObjectType.name = inputObjectTypes[0].getName();
        inputObjectType.description = inputObjectTypes[0].getDescription();
        inputObjectType.directives = io.vavr.collection.Stream.ofAll(Stream.of(inputObjectTypes).flatMap(item -> Stream.ofNullable(item.getDirectives()).flatMap(Collection::stream))).distinctBy(Directive::getName).collect(Collectors.toCollection(LinkedHashSet::new));
        inputObjectType.inputValues = inputObjectTypes[0].getInputValues();
        for (InputObjectType item : inputObjectTypes) {
            for (InputValue itemInputValue : item.getInputValues()) {
                if (inputObjectType.inputValues.stream().noneMatch(inputValue -> inputValue.getName().equals(itemInputValue.getName()))) {
                    inputObjectType.inputValues.add(itemInputValue);
                }
            }
        }
        return inputObjectType;
    }

    public String getName() {
        return name;
    }

    public InputObjectType setName(String name) {
        this.name = name;
        return this;
    }

    public Collection<Directive> getDirectives() {
        return directives;
    }

    public InputObjectType setDirectives(Collection<Directive> directives) {
        if (directives != null) {
            this.directives = new LinkedHashSet<>(directives);
        }
        return this;
    }

    public InputObjectType addDirective(Directive directive) {
        if (this.directives == null) {
            this.directives = new LinkedHashSet<>();
        }
        this.directives.add(directive);
        return this;
    }

    public InputObjectType addDirectives(Collection<Directive> directives) {
        if (this.directives == null) {
            this.directives = new LinkedHashSet<>();
        }
        this.directives.addAll(directives);
        return this;
    }

    public Collection<InputValue> getInputValues() {
        return inputValues;
    }

    public InputObjectType setInputValues(Collection<InputValue> inputValues) {
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

    public InputObjectType addInputValues(Collection<InputValue> inputValues) {
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
