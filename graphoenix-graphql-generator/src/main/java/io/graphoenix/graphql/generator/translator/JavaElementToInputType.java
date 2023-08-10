package io.graphoenix.graphql.generator.translator;

import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.operation.Directive;
import io.graphoenix.core.document.InputObjectType;
import io.graphoenix.core.document.InputValue;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.graphql.Ignore;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Types;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

import static io.graphoenix.core.utils.TypeNameUtil.TYPE_NAME_UTIL;
import static io.graphoenix.core.utils.ElementUtil.ELEMENT_UTIL;
import static io.graphoenix.spi.constant.Hammurabi.CLASS_INFO_DIRECTIVE_NAME;
import static io.graphoenix.spi.constant.Hammurabi.CONTAINER_TYPE_DIRECTIVE_NAME;

@ApplicationScoped
public class JavaElementToInputType {

    private final GraphQLConfig graphQLConfig;

    @Inject
    public JavaElementToInputType(GraphQLConfig graphQLConfig) {
        this.graphQLConfig = graphQLConfig;
    }

    public InputObjectType buildInputType(TypeElement typeElement, Types typeUtils) {
        return new InputObjectType()
                .setName(ELEMENT_UTIL.getNameFromElement(typeElement))
                .setDescription(ELEMENT_UTIL.getDescriptionFromElement(typeElement))
                .setInputValues(
                        typeElement.getEnclosedElements().stream()
                                .filter(element -> element.getKind().equals(ElementKind.FIELD))
                                .filter(element -> element.getAnnotation(Ignore.class) == null)
                                .map(element ->
                                        new InputValue()
                                                .setName(ELEMENT_UTIL.getNameFromElement(element))
                                                .setDescription(ELEMENT_UTIL.getDescriptionFromElement(element))
                                                .setDefaultValue(ELEMENT_UTIL.getDefaultValueFromElement(element))
                                                .setType(ELEMENT_UTIL.variableElementToTypeName((VariableElement) element, typeUtils))
                                )
                                .collect(Collectors.toCollection(LinkedHashSet::new))
                )
                .addDirective(
                        new Directive(CLASS_INFO_DIRECTIVE_NAME)
                                .addArgument("className", typeElement.getQualifiedName().toString())
                                .addArgument("exists", true)
                                .addArgument("annotationName", graphQLConfig.getAnnotationPackageName().concat(".").concat(typeElement.getSimpleName().toString()))
                                .addArgument("grpcClassName", graphQLConfig.getGrpcInputObjectTypePackageName().concat(".").concat(TYPE_NAME_UTIL.getGrpcTypeName(typeElement.getSimpleName().toString())))
                )
                .addDirective(
                        new Directive(CONTAINER_TYPE_DIRECTIVE_NAME)
                );
    }
}
