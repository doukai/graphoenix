package io.graphoenix.graphql.generator.translator;

import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.operation.Directive;
import io.graphoenix.core.document.Field;
import io.graphoenix.core.error.ElementProcessException;
import io.graphoenix.core.operation.ArrayValueWithVariable;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.graphql.Source;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.util.Types;
import java.util.Map;
import java.util.stream.Collectors;

import static io.graphoenix.core.error.ElementProcessErrorType.SOURCE_ANNOTATION_NOT_EXIST;
import static io.graphoenix.spi.constant.Hammurabi.*;

@ApplicationScoped
public class GraphQLApiBuilder {

    private final GraphQLConfig graphQLConfig;
    private final ElementManager elementManager;

    @Inject
    public GraphQLApiBuilder(GraphQLConfig graphQLConfig, ElementManager elementManager) {
        this.graphQLConfig = graphQLConfig;
        this.elementManager = elementManager;
    }

    public Field variableElementToField(ExecutableElement executableElement, Types typeUtils) {
        Field field = new Field()
                .setName(elementManager.getNameFromElement(executableElement))
                .setDescription(elementManager.getDescriptionFromElement(executableElement))
                .setType(elementManager.executableElementToTypeName(executableElement, typeUtils))
                .setArguments(elementManager.executableElementParametersToInputValues(executableElement, typeUtils))
                .addDirective(
                        new Directive()
                                .setName(INVOKE_DIRECTIVE_NAME)
                                .addArgument("className", executableElement.getEnclosingElement().toString())
                                .addArgument("methodName", executableElement.getSimpleName().toString())
                                .addArgument(
                                        "parameters",
                                        new ArrayValueWithVariable(
                                                executableElement.getParameters().stream()
                                                        .map(parameter -> Map.of("name", parameter.getSimpleName().toString(), "className", parameter.asType().toString()))
                                                        .collect(Collectors.toList())
                                        )
                                )
                                .addArgument("returnClassName", executableElement.getReturnType().toString())
                )
                .addDirective(
                        new Directive(PACKAGE_INFO_DIRECTIVE_NAME)
                                .addArgument("packageName", graphQLConfig.getPackageName())
                                .addArgument("grpcPackageName", graphQLConfig.getGrpcPackageName())
                );
        if (executableElement.getAnnotation(PermitAll.class) != null) {
            field.addDirective(new Directive(PERMIT_ALL));
        }
        if (executableElement.getAnnotation(DenyAll.class) != null) {
            field.addDirective(new Directive(DENY_ALL));
        }
        if (executableElement.getAnnotation(RolesAllowed.class) != null) {
            Directive directive = new Directive()
                    .setName(ROLES_ALLOWED)
                    .addArgument("roles", executableElement.getAnnotation(RolesAllowed.class).value());
            field.addDirective(directive);
        }
        return field;
    }

    public Tuple2<String, Field> variableElementToObjectField(ExecutableElement executableElement, Types typeUtils) {
        return Tuple.of(
                elementManager.variableElementToTypeName(
                        executableElement.getParameters().stream()
                                .filter(variableElement -> variableElement.getAnnotation(Source.class) != null)
                                .findFirst()
                                .orElseThrow(() -> new ElementProcessException(SOURCE_ANNOTATION_NOT_EXIST.bind(executableElement.getSimpleName()))),
                        typeUtils
                ),
                new Field()
                        .setName(elementManager.getSourceNameFromExecutableElement(executableElement).orElseGet(() -> elementManager.getNameFromElement(executableElement)))
                        .setDescription(elementManager.getDescriptionFromElement(executableElement))
                        .setType(elementManager.executableElementToTypeName(executableElement, typeUtils))
                        .setArguments(elementManager.executableElementParametersToInputValues(executableElement, typeUtils))
                        .addDirective(
                                new Directive()
                                        .setName(INVOKE_DIRECTIVE_NAME)
                                        .addArgument("className", executableElement.getEnclosingElement().toString())
                                        .addArgument("methodName", executableElement.getSimpleName().toString())
                                        .addArgument(
                                                "parameters",
                                                new ArrayValueWithVariable(
                                                        executableElement.getParameters().stream()
                                                                .map(parameter -> Map.of("name", parameter.getSimpleName().toString(), "className", parameter.asType().toString()))
                                                                .collect(Collectors.toList())
                                                )
                                        )
                                        .addArgument("returnClassName", executableElement.getReturnType().toString())
                        )
                        .addDirective(
                                new Directive(PACKAGE_INFO_DIRECTIVE_NAME)
                                        .addArgument("packageName", graphQLConfig.getPackageName())
                                        .addArgument("grpcPackageName", graphQLConfig.getGrpcPackageName())
                        )
        );
    }
}
