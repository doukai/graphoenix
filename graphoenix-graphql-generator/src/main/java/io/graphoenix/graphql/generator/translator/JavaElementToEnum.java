package io.graphoenix.graphql.generator.translator;

import io.graphoenix.graphql.generator.document.EnumType;
import io.graphoenix.graphql.generator.document.EnumValue;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import java.util.stream.Collectors;

public class JavaElementToEnum {

    public String buildEnum(TypeElement typeElement) {
        return new EnumType()
                .setEnumValues(
                        typeElement.getEnclosedElements().stream()
                                .filter(element -> element.getKind().equals(ElementKind.ENUM_CONSTANT))
                                .map(element -> new EnumValue().setName(element.getSimpleName().toString()))
                                .collect(Collectors.toList())
                ).toString();
    }
}
