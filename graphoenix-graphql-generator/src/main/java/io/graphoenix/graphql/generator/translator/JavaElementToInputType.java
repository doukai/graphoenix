package io.graphoenix.graphql.generator.translator;

import io.graphoenix.graphql.generator.document.InputObjectType;
import io.graphoenix.graphql.generator.document.InputValue;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Ignore;

import javax.inject.Inject;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Types;
import java.util.stream.Collectors;

public class JavaElementToInputType {

    private final ElementManager elementManager;

    @Inject
    public JavaElementToInputType(ElementManager elementManager) {
        this.elementManager = elementManager;
    }

    public String buildInputType(TypeElement typeElement, Types typeUtils) {
        return new InputObjectType()
                .setName(elementManager.getNameFormElement(typeElement))
                .setInputValues(
                        typeElement.getEnclosedElements().stream()
                                .filter(element -> element.getKind().equals(ElementKind.FIELD))
                                .filter(element -> element.getAnnotation(Ignore.class) == null)
                                .map(element ->
                                        new InputValue().setName(
                                                elementManager.getNameFormElement(element))
                                                .setTypeName(elementManager.typeElementToTypeName((VariableElement) element, typeUtils))
                                                .setDefaultValue(element.getAnnotation(DefaultValue.class) != null ? element.getAnnotation(DefaultValue.class).value() : null)
                                )
                                .collect(Collectors.toList())
                )
                .toString();
    }
}
