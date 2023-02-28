package io.graphoenix.graphql.generator.translator;

import io.graphoenix.core.document.Directive;
import io.graphoenix.core.document.EnumType;
import io.graphoenix.core.document.EnumValue;
import io.graphoenix.core.operation.Argument;
import io.graphoenix.core.operation.StringValue;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.graphql.Ignore;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

import static io.graphoenix.spi.constant.Hammurabi.CONTAINER_TYPE_DIRECTIVE_NAME;

@ApplicationScoped
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
                                .collect(Collectors.toCollection(LinkedHashSet::new))
                )
                .addDirective(
                        new Directive(CONTAINER_TYPE_DIRECTIVE_NAME)
                                .addArgument(
                                        new Argument()
                                                .setName("className")
                                                .setValueWithVariable(
                                                        new StringValue(typeElement.getQualifiedName().toString())
                                                )
                                )
                );
    }
}
