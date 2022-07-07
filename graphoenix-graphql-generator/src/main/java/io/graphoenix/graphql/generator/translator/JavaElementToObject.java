package io.graphoenix.graphql.generator.translator;

import io.graphoenix.core.document.Directive;
import io.graphoenix.core.document.Field;
import io.graphoenix.core.document.ObjectType;
import io.graphoenix.core.operation.Argument;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.graphql.DateFormat;
import org.eclipse.microprofile.graphql.Ignore;
import org.eclipse.microprofile.graphql.NumberFormat;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Types;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

@ApplicationScoped
public class JavaElementToObject {

    private final ElementManager elementManager;

    @Inject
    public JavaElementToObject(ElementManager elementManager) {
        this.elementManager = elementManager;
    }

    public ObjectType buildObject(TypeElement typeElement, Types typeUtils) {
        return new ObjectType()
                .setName(elementManager.getNameFromElement(typeElement))
                .setDescription(elementManager.getDescriptionFromElement(typeElement))
                .setFields(
                        typeElement.getEnclosedElements().stream()
                                .filter(element -> element.getKind().equals(ElementKind.FIELD))
                                .filter(element -> element.getAnnotation(Ignore.class) == null)
                                .map(element -> {
                                            Field field = new Field()
                                                    .setName(elementManager.getNameFromElement(element))
                                                    .setTypeName(elementManager.variableElementToTypeName((VariableElement) element, typeUtils))
                                                    .setDescription(elementManager.getDescriptionFromElement(element));
                                            NumberFormat numberFormat = element.getAnnotation(NumberFormat.class);
                                            if (numberFormat != null) {
                                                field.addDirective(
                                                        new Directive()
                                                                .setName("format")
                                                                .addArgument(new Argument("value", numberFormat.value()))
                                                                .addArgument(new Argument("locale", numberFormat.locale()))
                                                );
                                            }
                                            DateFormat dateFormat = element.getAnnotation(DateFormat.class);
                                            if (dateFormat != null) {
                                                field.addDirective(
                                                        new Directive()
                                                                .setName("format")
                                                                .addArgument(new Argument("value", dateFormat.value()))
                                                                .addArgument(new Argument("locale", dateFormat.locale()))
                                                );
                                            }
                                            return field;
                                        }
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
