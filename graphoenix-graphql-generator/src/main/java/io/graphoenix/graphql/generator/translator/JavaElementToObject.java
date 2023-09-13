package io.graphoenix.graphql.generator.translator;

import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.operation.Directive;
import io.graphoenix.core.document.Field;
import io.graphoenix.core.document.ObjectType;
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

import static io.graphoenix.core.utils.TypeNameUtil.TYPE_NAME_UTIL;
import static io.graphoenix.core.utils.ElementUtil.ELEMENT_UTIL;
import static io.graphoenix.spi.constant.Hammurabi.*;

@ApplicationScoped
public class JavaElementToObject {

    private final GraphQLConfig graphQLConfig;

    @Inject
    public JavaElementToObject(GraphQLConfig graphQLConfig) {
        this.graphQLConfig = graphQLConfig;
    }

    public ObjectType buildObject(TypeElement typeElement, Types typeUtils) {
        return new ObjectType()
                .setName(ELEMENT_UTIL.getNameFromElement(typeElement))
                .setDescription(ELEMENT_UTIL.getDescriptionFromElement(typeElement))
                .setFields(
                        typeElement.getEnclosedElements().stream()
                                .filter(element -> element.getKind().equals(ElementKind.FIELD))
                                .filter(element -> element.getAnnotation(Ignore.class) == null)
                                .map(element -> {
                                            Field field = new Field()
                                                    .setName(ELEMENT_UTIL.getNameFromElement(element))
                                                    .setType(ELEMENT_UTIL.variableElementToTypeName((VariableElement) element, typeUtils))
                                                    .setDescription(ELEMENT_UTIL.getDescriptionFromElement(element))
                                                    .addDirectives(ELEMENT_UTIL.getDirectivesFromElement(element));
                                            NumberFormat numberFormat = element.getAnnotation(NumberFormat.class);
                                            if (numberFormat != null) {
                                                field.addDirective(
                                                        new Directive()
                                                                .setName("format")
                                                                .addArgument("value", numberFormat.value())
                                                                .addArgument("locale", numberFormat.locale())
                                                );
                                            }
                                            DateFormat dateFormat = element.getAnnotation(DateFormat.class);
                                            if (dateFormat != null) {
                                                field.addDirective(
                                                        new Directive()
                                                                .setName("format")
                                                                .addArgument("value", dateFormat.value())
                                                                .addArgument("locale", dateFormat.locale())
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
                )
                .addDirectives(ELEMENT_UTIL.getDirectivesFromElement(typeElement))
                .addDirective(
                        new Directive(CLASS_INFO_DIRECTIVE_NAME)
                                .addArgument("className", typeElement.getQualifiedName().toString())
                                .addArgument("exists", true)
                                .addArgument("grpcClassName", graphQLConfig.getGrpcObjectTypePackageName() + "." + TYPE_NAME_UTIL.getGrpcTypeName(typeElement.getSimpleName().toString()))
                )
                .addDirective(
                        new Directive(CONTAINER_TYPE_DIRECTIVE_NAME)
                );
    }
}
