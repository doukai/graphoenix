package io.graphoenix.graphql.generator.translator;

import io.graphoenix.graphql.generator.document.Field;
import io.graphoenix.graphql.generator.document.InterfaceType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.graphql.Ignore;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Types;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

@ApplicationScoped
public class JavaElementToInterface {

    private final ElementManager elementManager;

    @Inject
    public JavaElementToInterface(ElementManager elementManager) {
        this.elementManager = elementManager;
    }

    public InterfaceType buildInterface(TypeElement typeElement, Types typeUtils) {
        return new InterfaceType()
                .setName(elementManager.getNameFromElement(typeElement))
                .setDescription(elementManager.getDescriptionFromElement(typeElement))
                .setFields(
                        typeElement.getEnclosedElements().stream()
                                .filter(element -> element.getKind().equals(ElementKind.FIELD))
                                .filter(element -> element.getAnnotation(Ignore.class) == null)
                                .map(element ->
                                        new Field()
                                                .setName(elementManager.getNameFromElement(element))
                                                .setTypeName(elementManager.variableElementToTypeName((VariableElement) element, typeUtils))
                                                .setDescription(elementManager.getDescriptionFromElement(element))
                                )
                                .collect(Collectors.toCollection(LinkedHashSet::new))
                )
                .setInterfaces(
                        typeElement.getInterfaces().stream()
                                .map(typeMirror -> typeUtils.asElement(typeMirror).getSimpleName().toString())
                                .collect(Collectors.toCollection(LinkedHashSet::new))
                );
    }
}
