package io.graphoenix.graphql.generator.translator;

import io.graphoenix.graphql.generator.document.Field;
import io.graphoenix.graphql.generator.document.InterfaceType;
import org.eclipse.microprofile.graphql.Ignore;

import javax.inject.Inject;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Types;
import java.util.stream.Collectors;

public class JavaElementToInterface {

    private final ElementManager elementManager;

    @Inject
    public JavaElementToInterface(ElementManager elementManager) {
        this.elementManager = elementManager;
    }

    public String buildInterface(TypeElement typeElement, Types typeUtils) {
        return new InterfaceType()
                .setName(elementManager.getNameFormElement(typeElement))
                .setFields(
                        typeElement.getEnclosedElements().stream()
                                .filter(element -> element.getKind().equals(ElementKind.FIELD))
                                .filter(element -> element.getAnnotation(Ignore.class) == null)
                                .map(element -> new Field().setName(elementManager.getNameFormElement(element)).setTypeName(elementManager.typeElementToTypeName((VariableElement) element, typeUtils)))
                                .collect(Collectors.toList())
                )
                .setInterfaces(
                        typeElement.getInterfaces().stream()
                                .map(typeMirror -> typeUtils.asElement(typeMirror).getSimpleName().toString())
                                .collect(Collectors.toList())
                )
                .toString();
    }
}
