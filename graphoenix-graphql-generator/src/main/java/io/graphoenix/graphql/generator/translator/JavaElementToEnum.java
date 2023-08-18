package io.graphoenix.graphql.generator.translator;

import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.operation.Directive;
import io.graphoenix.core.document.EnumType;
import io.graphoenix.core.document.EnumValue;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.graphql.Ignore;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

import static io.graphoenix.core.utils.TypeNameUtil.TYPE_NAME_UTIL;
import static io.graphoenix.core.utils.ElementUtil.ELEMENT_UTIL;
import static io.graphoenix.spi.constant.Hammurabi.CLASS_INFO_DIRECTIVE_NAME;
import static io.graphoenix.spi.constant.Hammurabi.CONTAINER_TYPE_DIRECTIVE_NAME;

@ApplicationScoped
public class JavaElementToEnum {

    private final GraphQLConfig graphQLConfig;

    @Inject
    public JavaElementToEnum(GraphQLConfig graphQLConfig) {
        this.graphQLConfig = graphQLConfig;
    }

    public EnumType buildEnum(TypeElement typeElement) {
        return new EnumType()
                .setName(ELEMENT_UTIL.getNameFromElement(typeElement))
                .setDescription(ELEMENT_UTIL.getDescriptionFromElement(typeElement))
                .setEnumValues(
                        typeElement.getEnclosedElements().stream()
                                .filter(element -> element.getKind().equals(ElementKind.ENUM_CONSTANT))
                                .filter(element -> element.getAnnotation(Ignore.class) == null)
                                .map(element ->
                                        new EnumValue()
                                                .setName(ELEMENT_UTIL.getNameFromElement(element))
                                                .setDescription(ELEMENT_UTIL.getDescriptionFromElement(element))
                                                .addDirectives(ELEMENT_UTIL.getDirectivesFromElement(element))
                                )
                                .collect(Collectors.toCollection(LinkedHashSet::new))
                )
                .addDirectives(ELEMENT_UTIL.getDirectivesFromElement(typeElement))
                .addDirective(
                        new Directive(CLASS_INFO_DIRECTIVE_NAME)
                                .addArgument("className", typeElement.getQualifiedName().toString())
                                .addArgument("exists", true)
                                .addArgument("grpcClassName", graphQLConfig.getGrpcEnumTypePackageName().concat(".").concat(TYPE_NAME_UTIL.getGrpcTypeName(typeElement.getSimpleName().toString())))
                )
                .addDirective(
                        new Directive(CONTAINER_TYPE_DIRECTIVE_NAME)
                );
    }
}
