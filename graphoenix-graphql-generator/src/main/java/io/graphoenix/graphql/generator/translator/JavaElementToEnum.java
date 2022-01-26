package io.graphoenix.graphql.generator.translator;

import io.graphoenix.graphql.generator.document.EnumType;
import io.graphoenix.graphql.generator.document.EnumValue;
import org.eclipse.microprofile.graphql.Ignore;

import javax.inject.Inject;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import java.util.stream.Collectors;

public class JavaElementToEnum {

    private final ElementManager elementManager;

    @Inject
    public JavaElementToEnum(ElementManager elementManager) {
        this.elementManager = elementManager;
    }

    public EnumType buildEnum(TypeElement typeElement) {
        return new EnumType()
                .setName(elementManager.getNameFromElement(typeElement))
                .setDescription(elementManager.getDescriptionFromElement(typeElement))
                .setEnumValues(
                        typeElement.getEnclosedElements().stream()
                                .filter(element -> element.getKind().equals(ElementKind.ENUM_CONSTANT))
                                .filter(element -> element.getAnnotation(Ignore.class) == null)
                                .map(element ->
                                        new EnumValue()
                                                .setName(elementManager.getNameFromElement(element))
                                                .setDescription(elementManager.getDescriptionFromElement(element))
                                )
                                .collect(Collectors.toSet())
                );
    }
}
