package io.graphoenix.graphql.generator.translator;

import io.graphoenix.graphql.generator.document.Field;
import io.graphoenix.graphql.generator.document.InputValue;

import javax.inject.Inject;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.util.Types;
import java.util.stream.Collectors;

public class GraphQLApiBuilder {

    private final ElementManager elementManager;

    @Inject
    public GraphQLApiBuilder(ElementManager elementManager) {
        this.elementManager = elementManager;
    }

    public Field variableElementToField(ExecutableElement executableElement, Types typeUtils) {
        return new Field()
                .setName(elementManager.getNameFromElement(executableElement))
                .setDescription(elementManager.getDescriptionFromElement(executableElement))
                .setTypeName(elementManager.executableElementToTypeName(executableElement, typeUtils))
                .setArguments(
                        executableElement.getParameters().stream()
                                .map(variableElement ->
                                        new InputValue()
                                                .setName(elementManager.getNameFromElement(variableElement))
                                                .setDescription(elementManager.getDescriptionFromElement(variableElement))
                                                .setDefaultValue(elementManager.getDefaultValueFromElement(variableElement))
                                                .setTypeName(elementManager.variableElementToInputTypeName(variableElement, typeUtils))
                                )
                                .collect(Collectors.toList())
                );
    }
}
